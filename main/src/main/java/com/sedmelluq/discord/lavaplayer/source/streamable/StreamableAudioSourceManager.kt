package com.sedmelluq.discord.lavaplayer.source.streamable

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.ThumbnailTools.getAsMetadata
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.NoRedirectsStrategy
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.ThreadLocalHttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder
import org.apache.commons.io.IOUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.jsoup.Jsoup
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern
import kotlin.math.ceil

class StreamableAudioSourceManager : HttpConfigurable, AudioSourceManager {
    private val httpInterfaceManager = ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    )

    val httpInterface: HttpInterface
        get() = httpInterfaceManager.`interface`

    override fun getSourceName() = "streamable"

    override fun loadItem(manager: AudioPlayerManager?, reference: AudioReference): AudioItem? {
        val m = STREAMABLE_REGEX.matcher(reference.identifier)

        if (!m.matches())
            return null

        val track = extractVideoUrlFromPage(reference)
        return track
//        return AudioReference("", "")
    }

    override fun isTrackEncodable(track: AudioTrack?) = true

    override fun encodeTrack(track: AudioTrack?, output: DataOutput?) {}

    override fun decodeTrack(trackInfo: AudioTrackInfo?, input: DataInput?) = StreamableAudioTrack(trackInfo, this)

    override fun shutdown() {}

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun extractVideoUrlFromPage(reference: AudioReference): AudioTrack {
        try {
            httpInterface.execute(HttpGet(reference.identifier)).use { response ->
                val html: String = IOUtils.toString(
                    response.entity.content,
                    StandardCharsets.UTF_8
                )
                val document = Jsoup.parse(html)
                val data = document.selectFirst("#player.container").children()
                    .first { it.hasAttr("data-shortcode") }
                val duration = ceil(data.attr("data-duration").toDouble()).toLong() * 1000L
                val artwork = document.selectFirst("meta[property=og:image]").attr("content")
                val trackInfo = AudioTrackInfoBuilder.empty()
                    .setUri(document.selectFirst("meta[property=og:url]").attr("content"))
                    .setAuthor("Unknown")
                    .setIsStream(false)
                    .setLength(duration)
                    .setIdentifier(data.attr("data-shortcode"))
                    .setTitle(data.attr("data-title"))
                    .setMetadata(getAsMetadata(artwork))
                    .build()
                return decodeTrack(trackInfo, null)
            }
        } catch (e: IOException) {
            throw FriendlyException("Failed to load info for streamable", FriendlyException.Severity.SUSPICIOUS, null)
        }
    }

    companion object {
        private val STREAMABLE_REGEX =
            Pattern.compile("^(?:http://|https://|)(?:www\\.|)(?:m\\.|)streamable\\.com/([a-zA-Z0-9-_]+)\$")
    }
}