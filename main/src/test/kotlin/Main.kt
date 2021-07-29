import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

fun main() {
    val playerManager = DefaultAudioPlayerManager()
    AudioSourceManagers.registerRemoteSources(playerManager)
    playerManager.loadItem("https://sallyspitz.bandcamp.com/track/unchained", object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            println(track.info.title)
            println(track.info.uri)
            println(track.info.artworkUrl)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            val track = playlist.tracks.firstOrNull()
                ?: return println("Empty playlist received")
            println(track.info.title)
            println(track.info.uri)
            println(track.info.artworkUrl)
        }

        override fun noMatches() {
            println("No matching items found")
        }

        override fun loadFailed(exception: FriendlyException) {
            println(exception.stackTraceToString())
        }
    }).get()
}