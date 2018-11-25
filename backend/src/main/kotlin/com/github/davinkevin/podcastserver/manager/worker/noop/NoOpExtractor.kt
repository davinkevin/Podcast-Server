package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.entity.Item

/**
 * Created by kevin on 03/12/2017
 */
class NoOpExtractor : Extractor {

    override fun extract(item: Item) =
            DownloadingItem(item, listOf(item.url!!), null, null)

    override fun compatibility(url: String?) = Integer.MAX_VALUE
}
