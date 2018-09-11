package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.downloader.DownloadingItem
import lan.dk.podcastserver.manager.worker.Extractor
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.lang.Integer.MAX_VALUE

/**
 * Created by kevin on 03/12/2017
 */
@Component
@Scope("prototype")
class PassThroughExtractor : Extractor {

    override fun extract(item: Item) =
            DownloadingItem(item, listOf(item.url).toVΛVΓ(), null, null)

    override fun compatibility(url: String?) = MAX_VALUE - 1

}
