package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.podcastserver.item.Sluggable
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*
import kotlin.math.ceil

/**
 * Created by kevin on 2019-02-09
 */
data class DeleteItemRequest(val id: UUID, val fileName: Path, val podcastTitle: String) {
    val path: Path = Paths.get(podcastTitle).resolve(fileName)
}

data class Item(
        val id: UUID,
        override val title: String,
        val url: String?,

        val pubDate: OffsetDateTime?,
        val downloadDate: OffsetDateTime?,
        val creationDate: OffsetDateTime?,

        val description: String?,
        override val mimeType: String,
        val length: Long?,
        override val fileName: Path?,
        val status: Status,

        val podcast: Podcast,
        val cover: Cover
): Sluggable {

    fun isDownloaded() = Status.FINISH == status && fileName != null

    data class Podcast(val id: UUID, val title: String, val url: String?)
    data class Cover(val id: UUID, val url: URI, val width: Int, val height: Int)
}

data class ItemSort(val direction: String, val field: String)
data class ItemPageRequest(val page: Int, val size: Int, val sort: ItemSort)

data class PageItem(
        val content: Collection<Item>,
        val empty: Boolean,
        val first: Boolean,
        val last: Boolean,
        val number: Int,
        val numberOfElements: Int,
        val size: Int,
        val totalElements: Int,
        val totalPages: Int
) {
    companion object {

        fun of(content: Collection<Item>, totalElements: Int, page: ItemPageRequest): PageItem {

            val totalPages = ceil(totalElements.toDouble() / page.size.toDouble()).toInt()

            return PageItem(
                    content = content,
                    empty = content.isEmpty(),
                    first = page.page == 0,
                    last = page.page + 1 > totalPages - 1,
                    number = page.page,
                    numberOfElements = content.size,
                    size = page.size,
                    totalElements = totalElements,
                    totalPages = totalPages
            )
        }

    }
}

data class ItemForCreation(
        val title: String,
        val url: String?,
        val guid: String,

        val pubDate: OffsetDateTime,
        val downloadDate: OffsetDateTime?,
        val creationDate: OffsetDateTime,

        val description: String?,
        val mimeType: String,
        val length: Long?,
        val fileName: Path?,
        val status: Status,

        val podcastId: UUID,
        val cover: CoverForCreation?
)

data class ItemPlaylist(val id: UUID, val name: String)
