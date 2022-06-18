package com.github.davinkevin.podcastserver.manager.downloader

import com.github.davinkevin.podcastserver.entity.Status
import java.net.URI
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Created by kevin on 03/12/2017
 */
data class DownloadingInformation(val item: DownloadingItem, val urls: List<URI>, val filename: Path, val userAgent: String?) {

    fun url(): URI = urls.firstOrNull() ?: item.url

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
        val newFileName = Path(filename)
        val extension = newFileName.extension
        val base = newFileName.nameWithoutExtension

        val safeFilename = base
            .replace("\n".toRegex(), "")
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            .take(100)
            .plus(".$extension")
            .let(::Path)

        return this.copy(filename = safeFilename)
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
