package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.update.updaters.Type
import java.net.URI

/**
 * Created by kevin on 13/09/2018
 */

internal const val PLAYLIST_URL_PART = "playlist?list="

internal fun isPlaylist(url: URI) = url.toASCIIString().contains(PLAYLIST_URL_PART)

internal val type = Type("Youtube", "Youtube")
internal fun youtubeCompatibility(url: String) = when {
        "youtube.com/" in url -> 1
        else -> Integer.MAX_VALUE
}

