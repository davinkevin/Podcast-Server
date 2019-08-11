package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.manager.worker.Finder
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Created by kevin on 2019-08-11
 */
class FindService(val finders: Set<Finder>) {

    fun find(url: URI): Mono<FindPodcastInformation> {
        val finder = finders.minBy { it.compatibility(url.toASCIIString()) }!!
        return finder.findInformation(url.toASCIIString())
    }

}
