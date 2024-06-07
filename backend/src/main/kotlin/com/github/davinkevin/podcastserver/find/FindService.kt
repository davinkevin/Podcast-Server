package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.find.finders.Finder
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Created by kevin on 2019-08-11
 */
class FindService(
    val finders: Set<Finder>
) {
    private val log = LoggerFactory.getLogger(FindService::class.java)

    fun find(url: URI): FindPodcastInformation {
        val finder = finders.minBy { it.compatibility(url.toASCIIString()) }

        return finder.findInformationSync(url.toASCIIString())
            ?: FindPodcastInformation(title = "", url = url, type = "RSS", cover = null, description = "")
    }

    private fun Finder.findInformationSync(url: String): FindPodcastInformation? {
        return runCatching { findInformation(url).block() }
            .onFailure { log.error("error during execution of finder", it) }
            .getOrNull()
    }
}

