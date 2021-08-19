package com.sedmelluq.discord.lavaplayer.track

/**
 * Meta info for an audio track
 * @param title Track title
 * @param author Track author, if known
 * @param length Length of the track in milliseconds, UnitConstants.DURATION_MS_UNKNOWN for streams.
 * @param identifier Audio source specific identifier.
 * @param isStream True if this track is a stream.
 * @param uri URL of the track, or local path to the file.
 * @param metadata Additional metadata of the track.
 */
data class AudioTrackInfo(
    @JvmField val title: String,
    @JvmField val author: String,
    @JvmField val length: Long,
    @JvmField val identifier: String,
    @JvmField val isStream: Boolean,
    @JvmField val uri: String,
    @JvmField val metadata: Map<String, String>?
) {
    /**
     * @return Artwork URL of the track
     */
    val artworkUrl: String?
        get() =
            metadata?.get("artworkUrl")
}
