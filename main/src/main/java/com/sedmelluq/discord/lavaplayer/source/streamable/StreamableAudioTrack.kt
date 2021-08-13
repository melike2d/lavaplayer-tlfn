package com.sedmelluq.discord.lavaplayer.source.streamable

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.Units
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import org.slf4j.LoggerFactory
import java.net.URI

class StreamableAudioTrack(
    trackInfo: AudioTrackInfo?,
    private val sourceManager: StreamableAudioSourceManager
) : DelegatedAudioTrack(trackInfo) {
    private val log = LoggerFactory.getLogger(DelegatedAudioTrack::class.java)

    override fun process(localExecutor: LocalAudioTrackExecutor?) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug("Starting streamable track from URL: {}", trackInfo.identifier)
            PersistentHttpStream(
                httpInterface,
                URI(trackInfo.identifier),
                Units.CONTENT_LENGTH_UNKNOWN
            ).use { inputStream -> processDelegate(MpegAudioTrack(trackInfo, inputStream), localExecutor) }
        }
    }

    override fun getSourceManager() = this.sourceManager
}