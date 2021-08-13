package com.sedmelluq.discord.lavaplayer.source.streamable

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.charset.StandardCharsets

class StreamableAudioTrack(
    trackInfo: AudioTrackInfo?,
    private val sourceManager: StreamableAudioSourceManager
) : DelegatedAudioTrack(trackInfo) {
    private val log = LoggerFactory.getLogger(DelegatedAudioTrack::class.java)

    override fun process(localExecutor: LocalAudioTrackExecutor?) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug("Loading Streamable track page from URL: {}", trackInfo.uri)
            val trackUrl = getTrackMediaUrl(httpInterface)
            log.debug("Starting streamable track from URL: {}", trackUrl)
            PersistentHttpStream(
                httpInterface, URI(trackUrl), null
            ).use { inputStream -> processDelegate(MpegAudioTrack(trackInfo, inputStream), localExecutor) }
        }
    }

    private fun getTrackMediaUrl(httpInterface: HttpInterface): String {
        httpInterface.execute(HttpGet(trackInfo.uri)).use { response ->
            HttpClientTools.assertSuccessWithContent(response, "track page")
            val responseText = IOUtils.toString(
                response.entity.content,
                StandardCharsets.UTF_8
            )
            val document = Jsoup.parse(responseText)
            return document.selectFirst("meta[property=og:url]").attr("content")
        }
    }

    override fun makeShallowClone() = StreamableAudioTrack(trackInfo, sourceManager)

    override fun getSourceManager() = this.sourceManager
}