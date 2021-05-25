package net.shilu.lavaplayer.testing

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

object SearchingTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val pm = DefaultAudioPlayerManager()
        pm.registerSourceManager(YoutubeAudioSourceManager())
        pm.loadItem("ytsearch:Never Done This", object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                println(track.info.artworkUrl)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                println(playlist.tracks.first().info.artworkUrl)
            }

            override fun noMatches() {
                println(NullPointerException())
            }

            override fun loadFailed(exception: FriendlyException) {
                throw exception
            }
        })
    }
}