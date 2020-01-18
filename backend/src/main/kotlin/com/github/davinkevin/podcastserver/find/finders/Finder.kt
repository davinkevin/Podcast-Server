package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import reactor.core.publisher.Mono

/**
 * Created by kevin on 22/02/15.
 */
interface Finder {
    fun findInformation(url: String): Mono<FindPodcastInformation>
    fun compatibility(url: String?): Int
}
