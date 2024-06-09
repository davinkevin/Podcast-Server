package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.find.finders.Finder
import java.net.URI

/**
 * Created by kevin on 2019-08-11
 */
class FindService(
    val finders: Set<Finder>
) {
    fun find(url: URI): FindPodcastInformation {
        val finder = finders.minBy { it.compatibility(url.toASCIIString()) }

        return finder.findPodcastInformation(url.toASCIIString())
            ?: FindPodcastInformation(title = "", url = url, type = "RSS", cover = null, description = "")
    }
}

