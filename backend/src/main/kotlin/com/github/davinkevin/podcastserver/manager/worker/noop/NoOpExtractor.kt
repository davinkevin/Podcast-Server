package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor

/**
 * Created by kevin on 03/12/2017
 */
class NoOpExtractor : Extractor {

    override fun extract(item: Item) =
            DownloadingItem(item, listOf(item.url).toVΛVΓ(), null, null)

    override fun compatibility(url: String?) = Integer.MAX_VALUE
}
