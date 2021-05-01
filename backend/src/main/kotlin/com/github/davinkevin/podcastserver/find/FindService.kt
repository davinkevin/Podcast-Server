package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.noop.NoOpFinder
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 2019-08-11
 */
class FindService(val finders: Set<Finder>) {

    private val log = LoggerFactory.getLogger(FindService::class.java)

    suspend fun find(url: URI): FindPodcastInformation {
        val finder = finders.minByOrNull { it.compatibility(url.toASCIIString()) }
            ?: NoOpFinder()

        log.info("finder selected is {} for {}", finder.javaClass, url.toASCIIString())

        return runCatching { finder.findInformation(url.toASCIIString()).awaitFirst() }
            .getOrDefault(FindPodcastInformation(title = "", url = url, type = "RSS", cover = null, description = ""))
    }

}
