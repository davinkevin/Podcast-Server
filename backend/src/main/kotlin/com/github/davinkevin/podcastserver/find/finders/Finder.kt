package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

private val log = LoggerFactory.getLogger(Finder::class.java)
/**
 * Created by kevin on 22/02/15.
 */
interface Finder {
    fun findPodcastInformation(url: String): FindPodcastInformation? {
        return runCatching { findInformation(url).block() }
            .onFailure { log.error("error during execution of finder", it) }
            .getOrNull()
    }
    fun findInformation(url: String): Mono<FindPodcastInformation>
    fun compatibility(url: String): Int
}
