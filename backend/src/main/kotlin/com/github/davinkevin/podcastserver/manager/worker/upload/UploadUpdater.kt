package com.github.davinkevin.podcastserver.manager.worker.upload

import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import org.springframework.stereotype.Component
import java.util.function.Predicate

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Component
class UploadUpdater : Updater {

    override fun getItems(podcast: Podcast) = podcast.items.toVΛVΓ()

    override fun signatureOf(podcast: Podcast) = ""

    override fun notIn(podcast: Podcast)= Predicate<Item> { java.lang.Boolean.FALSE }

    override fun type(): Type = TYPE

    override fun compatibility(url: String?) = Integer.MAX_VALUE

    companion object {
        val TYPE = Type("upload", "Upload")
    }
}
