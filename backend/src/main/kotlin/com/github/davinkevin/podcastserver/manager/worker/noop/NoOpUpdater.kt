package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
class NoOpUpdater : Updater {

    override fun update(podcast: Podcast) = Updater.NO_MODIFICATION

    override fun findItems(podcast: Podcast) = setOf<Item>()

    override fun signatureOf(podcast: Podcast) = ""

    override fun notIn(podcast: Podcast): (Item) -> Boolean = { false }

    override fun type() = Type("NoOpUpdater", "NoOpUpdater")

    override fun compatibility(url: String?) = Integer.MAX_VALUE
}
