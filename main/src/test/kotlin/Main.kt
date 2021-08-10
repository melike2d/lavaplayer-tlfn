import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

fun main() {
    val playerManager = DefaultAudioPlayerManager()
    AudioSourceManagers.registerRemoteSources(playerManager)
    val query = "https://soundcloud.app.goo.gl/Zv4azcTV2Ef9FnUr8"
    playerManager.loadItem(query, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            println(track.info.title)
            println(track.info.uri)
            println(track.duration)
            println(track.info.artworkUrl)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            playlist.tracks.firstOrNull()
                ?: return println("Empty playlist received")
            for (track in playlist.tracks) {
                println(track.info.title)
                println(track.info.author)
                println(track.info.uri)
                println(track.duration)
                println(track.info.artworkUrl)
                println("-----")
            }
        }

        override fun noMatches() {
            println("No matching items found")
        }

        override fun loadFailed(exception: FriendlyException) {
            println(exception.stackTraceToString())
        }
    }).get()
}