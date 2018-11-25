package com.github.davinkevin.podcastserver.manager.worker.upload

import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import org.springframework.stereotype.Component

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Component
class UploadUpdater : Updater {

    override fun findItems(podcast: Podcast) = podcast.items!!

    override fun signatureOf(podcast: Podcast) = ""

    override fun notIn(podcast: Podcast): (Item) -> Boolean = { false }

    override fun type(): Type = TYPE

    override fun compatibility(url: String?) = Integer.MAX_VALUE

    companion object {
        val TYPE = Type("upload", "Upload")
    }
}
