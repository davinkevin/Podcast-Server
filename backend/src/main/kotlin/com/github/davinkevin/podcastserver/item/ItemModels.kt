package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.entity.Status
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
class DeleteItemInformation(val id: UUID, fileName: String, podcastTitle: String) {
    val path = Paths.get(podcastTitle, fileName)!!
}

data class Item(
        val id: UUID,
        val title: String,
        val url: String,

        val pubDate: OffsetDateTime?,
        val downloadDate: OffsetDateTime?,
        val creationDate: OffsetDateTime?,

        val description: String,
        val mimeType: String?,
        val length: Long?,
        val fileName: String?,
        val status: Status,

        val podcast: PodcastForItem,
        val cover: CoverForItem
) {
    fun isDownloaded() = Status.FINISH == status
}

data class PodcastForItem(
        val id: UUID,
        val title: String,
        val url: String
)

data class CoverForItem (
        val id: UUID,
        val url: String,
        val width: Int,
        val height: Int
)
