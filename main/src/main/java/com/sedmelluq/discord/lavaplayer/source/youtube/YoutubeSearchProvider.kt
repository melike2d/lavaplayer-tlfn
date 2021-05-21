package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.methods.HttpGet
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import org.jsoup.Jsoup
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import kotlin.Throws
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Handles processing YouTube searches.
 */
class YoutubeSearchProvider : YoutubeSearchResultLoader {
    private val httpInterfaceManager: HttpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager()
    private val polymerInitialDataRegex =
        Pattern.compile("(window\\[\"ytInitialData\"]|var ytInitialData)\\s*=\\s*(.*);")

    override fun getHttpConfiguration(): ExtendedHttpConfigurable = httpInterfaceManager

    /**
     * @param query Search query.
     * @return Playlist of the first page of results.
     */
    override fun loadSearchResult(query: String, trackFactory: Function<AudioTrackInfo, AudioTrack>): AudioItem {
        log.debug("Performing a search with query {}", query)
        try {
            httpInterfaceManager.getInterface().use { httpInterface ->
                val url = URIBuilder("https://www.youtube.com/results")
                    .addParameter("search_query", query)
                    .addParameter("hl", "en")
                    .addParameter("persist_hl", "1").build()
                httpInterface.execute(HttpGet(url)).use { response ->
                    HttpClientTools.assertSuccessWithContent(response, "search response")
                    val document = Jsoup.parse(response.entity.content, StandardCharsets.UTF_8.name(), "")
                    return extractSearchResults(document, query, trackFactory)
                }
            }
        } catch (e: Throwable) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e)
        }
    }

    private fun extractSearchResults(
        document: Document, query: String,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioItem {
        val tracks = mutableListOf<AudioTrack>()
        val resultsSelection = document.select("#page > #content #results")
        if (!resultsSelection.isEmpty()) {
            for (results in resultsSelection) {
                for (result in results.select(".yt-lockup-video")) {
                    if (!result.hasAttr("data-ad-impressions")
                        && result.select(".standalone-ypc-badge-renderer-label").isEmpty()
                    ) {
                        extractTrackFromResultEntry(tracks, result, trackFactory)
                    }
                }
            }
        } else {
            log.debug("Attempting to parse results page as polymer")
            try {
                tracks.addAll(polymerExtractTracks(document, trackFactory))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        return if (tracks.isEmpty()) {
            AudioReference.NO_TRACK
        } else {
            BasicAudioPlaylist("Search results for: $query", tracks, null, true)
        }
    }

    private fun extractTrackFromResultEntry(
        tracks: MutableList<AudioTrack>, element: Element,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ) {
        val durationElement = element.select("[class^=video-time]").first()
        val contentElement = element.select(".yt-lockup-content").first()
        val videoId = element.attr("data-context-item-id")
        if (durationElement == null || contentElement == null || videoId.isEmpty()) {
            return
        }
        val duration = DataFormatTools.durationTextToMillis(durationElement.text())
        val title = contentElement.select(".yt-lockup-title > a").text()
        val author = contentElement.select(".yt-lockup-byline > a").text()
        val info = AudioTrackInfo(title, author, duration, videoId, false, WATCH_URL_PREFIX + videoId)
        tracks.add(trackFactory.apply(info))
    }

    @Throws(IOException::class)
    private fun polymerExtractTracks(
        document: Document,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): MutableList<AudioTrack> {
        // Match the JSON from the HTML. It should be within a script tag
        val matcher = polymerInitialDataRegex.matcher(document.outerHtml())
        if (!matcher.find()) {
            log.warn("Failed to match ytInitialData JSON object")
            return mutableListOf()
        }

        val jsonBrowser = JsonBrowser.parse(matcher.group(2))
        val tracks = mutableListOf<AudioTrack>()
        jsonBrowser["contents"]["twoColumnSearchResultsRenderer"]["primaryContents"]["sectionListRenderer"]["contents"]
            .index(0)["itemSectionRenderer"]["contents"]
            .values()
            .mapNotNull { extractPolymerData(it, trackFactory) }
            .forEach(tracks::add)
        return tracks
    }

    private fun extractPolymerData(json: JsonBrowser, trackFactory: Function<AudioTrackInfo, AudioTrack>): AudioTrack? {
        val renderer = json["videoRenderer"]
        if (renderer.isNull) { // Not a track, ignore
            return null
        }

        val videoId = renderer["videoId"].text()
        val title = renderer["title"]["runs"].index(0)["text"].text()
        val author = renderer["ownerText"]["runs"].index(0)["text"].text()
        val lengthText = renderer["lengthText"]["simpleText"].text()
        val isStream = lengthText == null
        val duration = if (isStream) LIVE_STREAM_DURATION else DataFormatTools.durationTextToMillis(lengthText)
        val info = AudioTrackInfo(title, author, duration, videoId, isStream, WATCH_URL_PREFIX + videoId)
        return trackFactory.apply(info)
    }

    companion object {
        private val log = LoggerFactory.getLogger(YoutubeSearchProvider::class.java)
        private const val LIVE_STREAM_DURATION = Long.MAX_VALUE
        private const val WATCH_URL_PREFIX = "https://www.youtube.com/watch?v="
    }

    init {
        httpInterfaceManager.setHttpContextFilter(BaseYoutubeHttpContextFilter())
    }
}