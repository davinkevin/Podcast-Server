package com.github.davinkevin.podcastserver.manager.worker.noop

import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
class NoOpFinder : Finder {
    override fun find(url: String): Podcast = Podcast.DEFAULT_PODCAST
    override fun compatibility(url: String?) = -1
}
