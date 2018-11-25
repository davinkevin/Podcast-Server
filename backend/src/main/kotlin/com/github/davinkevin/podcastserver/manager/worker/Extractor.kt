package com.github.davinkevin.podcastserver.manager.worker

import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.apache.commons.io.FilenameUtils

/**
 * Created by kevin on 03/12/2017
 */
interface Extractor {

    fun extract(item: Item): DownloadingItem
    fun compatibility(url: String?): Int

    fun getFileName(item: Item): String =
            item.url.toOption()
                    .map { it.substringBefore("?") }
                    .map { FilenameUtils.getName(it) }
                    .getOrElse { "" }
}
