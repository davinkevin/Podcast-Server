package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

/**
 * Created by kevin on 08/05/2020
 */

internal fun isFromVideoPlatform(url: String): Boolean = when {
    "youtube.com" in url -> true
    "www.6play.fr" in url -> true
    "www.tf1.fr" in url -> true
    "www.france.tv" in url -> true
    "replay.gulli.fr" in url -> true
    "dailymotion.com" in url -> true
    else -> false
}
