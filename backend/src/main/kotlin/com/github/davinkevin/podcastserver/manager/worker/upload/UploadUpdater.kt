package com.github.davinkevin.podcastserver.manager.worker.upload

import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.manager.worker.ItemFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Component
class UploadUpdater : Updater {

    override fun blockingFindItems(podcast: PodcastToUpdate) = setOf<ItemFromUpdate>()

    override fun blockingSignatureOf(url: URI) = ""

    override fun type(): Type = TYPE

    override fun compatibility(url: String?) = Integer.MAX_VALUE

    companion object {
        val TYPE = Type("upload", "Upload")
    }
}
