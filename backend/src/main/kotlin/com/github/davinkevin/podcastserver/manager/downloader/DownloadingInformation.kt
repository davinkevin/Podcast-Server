package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.entity.Status
import java.net.URI
import java.util.*

/**
 * Created by kevin on 03/12/2017
 */
data class DownloadingInformation(val item: DownloadingItem, val urls: List<String>, val filename: String, val userAgent: String?) {
    fun url(): String = urls.firstOrNull() ?: item.url.toASCIIString()

    fun status(status: Status): DownloadingInformation {
        val newItem = this.item.copy(status = status)
        return this.copy(item = newItem)
    }

    fun addATry(): DownloadingInformation {
        val newItem = this.item.copy(numberOfFail = this.item.numberOfFail + 1)
        return this.copy(item = newItem)
    }

    fun progression(progression: Int): DownloadingInformation {
        val newItem = this.item.copy(progression = progression)
        return this.copy(item = newItem)
    }

    fun fileName(filename: String): DownloadingInformation {
        return this.copy(filename = filename)
    }
}

data class DownloadingItem(
        val id: UUID,
        val title: String,
        val status: Status,
        val url: URI,
        val numberOfFail: Int,
        val progression: Int,
        val podcast: Podcast,
        val cover: Cover
) {
    data class Podcast(val id: UUID, val title: String)
    data class Cover(val id: UUID, val url: URI)
}
