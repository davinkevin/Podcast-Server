package com.github.davinkevin.podcastserver.find.finders.noop

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
class NoOpFinder : Finder {

    private val log = LoggerFactory.getLogger(NoOpFinder::class.java)

    override fun findPodcastInformation(url: String): FindPodcastInformation {
        log.warn("Using Noop finder for url {}", url)

        return FindPodcastInformation(
            title = "",
            url = URI(url),
            description = "",
            type = "noop",
            cover = null
        )
    }

    override fun compatibility(url: String): Int = Int.MAX_VALUE
}
