package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import java.net.URI

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
class NoOpUpdater : Updater {

    override fun update(podcast: Podcast) = Updater.NO_MODIFICATION

    override fun findItems(podcast: Podcast) = setOf<Item>()

    override fun signatureOf(url: URI) = ""

    override fun notIn(podcast: Podcast): (Item) -> Boolean = { false }

    override fun type() = Type("NoOpUpdater", "NoOpUpdater")

    override fun compatibility(url: String?) = Integer.MAX_VALUE
}
