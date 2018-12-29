package com.github.davinkevin.podcastserver.manager.selector

import lan.dk.podcastserver.manager.worker.Finder
import lan.dk.podcastserver.manager.worker.noop.NoOpFinder
import org.springframework.stereotype.Service

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service
class FinderSelector(val finders: Set<Finder>) {

    fun of(url: String?): Finder =
            if (url.isNullOrEmpty())
                NO_OP_FINDER
            else {
                finders.minBy { it.compatibility(url) }!!
            }

    companion object {
        val NO_OP_FINDER = NoOpFinder()
    }
}
