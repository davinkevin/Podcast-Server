package com.github.davinkevin.podcastserver.manager.worker

import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 22/02/15.
 */
interface Finder {
    fun find(url: String): Podcast

    fun findInformation(url: String): Mono<FindPodcastInformation> {
        val p = find(url)
        val c = p.cover!!

        return FindPodcastInformation(
                title = p.title!!,
                url = URI(p.url!!),
                type = p.type!!,
                cover = FindCoverInformation(
                        url = URI(c.url!!),
                        width = c.width!!,
                        height = c.height!!
                )
        ).toMono()
    }

    fun compatibility(url: String?): Int
}
