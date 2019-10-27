package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.manager.worker.*
import reactor.kotlin.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
class NoOpUpdater : Updater {

    override fun update(podcast: PodcastToUpdate) = NO_MODIFICATION.toMono()

    override fun blockingFindItems(podcast: PodcastToUpdate) = setOf<ItemFromUpdate>()

    override fun blockingSignatureOf(url: URI) = ""

    override fun type() = Type("NoOpUpdater", "NoOpUpdater")

    override fun compatibility(url: String?) = Integer.MAX_VALUE
}
