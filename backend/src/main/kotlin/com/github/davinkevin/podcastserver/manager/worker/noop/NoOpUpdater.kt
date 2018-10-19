package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import java.util.function.Predicate

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
class NoOpUpdater : Updater {

    override fun update(podcast: Podcast) = Updater.NO_MODIFICATION_TUPLE

    override fun getItems(podcast: Podcast) = setOf<Item>().toVΛVΓ()

    override fun signatureOf(podcast: Podcast) = ""

    override fun notIn(podcast: Podcast)= Predicate<Item>{ false }

    override fun type() = Type("NoOpUpdater", "NoOpUpdater")

    override fun compatibility(url: String?) = Integer.MAX_VALUE
}
