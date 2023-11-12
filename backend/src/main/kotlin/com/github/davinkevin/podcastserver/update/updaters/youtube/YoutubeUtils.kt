package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.update.updaters.Type
import java.net.URI

internal fun isPlaylist(url: URI) = url.toASCIIString().contains("playlist?list=")
internal fun isHandle(url: URI): Boolean {
        return url.path.matches("^/@[^/]*$".toRegex()) || url.path.startsWith("/c/")
}
internal fun isChannel(url: URI) = url.toASCIIString().matches(".*/channel/UC.*".toRegex())

internal val type = Type("Youtube", "Youtube")
internal fun youtubeCompatibility(url: String) = when {
        "youtube.com/" in url -> 1
        else -> Integer.MAX_VALUE
}

