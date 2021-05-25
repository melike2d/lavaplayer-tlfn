package net.shilu.lavaplayer.testing

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.concurrent.CompletableFuture

object SearchingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val pm = DefaultAudioPlayerManager()
        pm.registerSourceManager(YoutubeAudioSourceManager())
        val future = CompletableFuture<String>()
        pm.loadItem("ytmsearch:Never Done This", object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                future.complete(track.info.artworkUrl)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                future.complete(playlist.tracks.first().info.artworkUrl)
            }

            override fun noMatches() {
                future.completeExceptionally(NullPointerException())
            }

            override fun loadFailed(exception: FriendlyException) {
                future.completeExceptionally(exception)
            }
        })
        println(future.get())
    }
}