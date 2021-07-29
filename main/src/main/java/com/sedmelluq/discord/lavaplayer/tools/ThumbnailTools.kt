package com.sedmelluq.discord.lavaplayer.tools

object ThumbnailTools {
    @JvmStatic
    fun extractYouTube(jsonBrowser: JsonBrowser, videoId: String): String {
        val thumbnails = jsonBrowser.get("thumbnail").get("thumbnails").values()
        val thumbnail = thumbnails.maxByOrNull {
            it.get("width").asLong(0) + it.get("height").asLong(0)
        } ?: return "https://img.youtube.com/vi/$videoId/0.jpg"
        return thumbnail.get("url").text()
    }

    @JvmStatic
    fun extractSoundCloud(jsonBrowser: JsonBrowser): String {
        val thumbnail = jsonBrowser.get("artwork_url")
        return (if (!thumbnail.isNull) thumbnail.text()
        else jsonBrowser.get("user").get("avatar_url").text())
            .replace("large.jpg", "original.jpg")
    }
}