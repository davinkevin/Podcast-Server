package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.manager.worker.Finder
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 2019-08-11
 */
class FindService(val finders: Set<Finder>) {

    private val log = LoggerFactory.getLogger(FindService::class.java)

    fun find(url: URI): Mono<FindPodcastInformation> {
        val finder = finders.minBy { it.compatibility(url.toASCIIString()) }!!

        log.debug("finder selected is {}", finder.javaClass)

        return finder
                .findInformation(url.toASCIIString())
                .onErrorResume {
                    log.error("error during execution of finder", it)

                    FindPodcastInformation(title = "", url = url, type = "RSS", cover = null, description = "")
                            .toMono()
                }
    }

}
