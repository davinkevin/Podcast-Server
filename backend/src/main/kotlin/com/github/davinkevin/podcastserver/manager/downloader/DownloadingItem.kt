package com.github.davinkevin.podcastserver.manager.downloader

import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.entity.Item

/**
 * Created by kevin on 03/12/2017
 */
data class DownloadingItem(val item: Item, val urls: List<String>, val filename: String?, val userAgent: String?) {
    fun url()= urls.firstOption()
}
