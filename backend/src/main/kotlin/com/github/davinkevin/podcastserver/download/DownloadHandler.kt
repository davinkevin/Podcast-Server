package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import org.springframework.web.servlet.function.paramOrNull
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*

/**
 * Created by kevin on 17/09/2019
 */
class DownloadHandler(private val downloadService: ItemDownloadManager) {

    fun download(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))

        downloadService.addItemToQueue(id)

        return ServerResponse.noContent().build()
    }

    fun downloading(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val body = downloadService.downloading
            .map(::toDownloadingItem)
            .let(::DownloadingItemsHAL)

        return ServerResponse.ok().body(body)
    }

    fun findLimit(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse =
            ServerResponse.ok().body(downloadService.limitParallelDownload)

    fun updateLimit(r: ServerRequest): ServerResponse {
        val limit = r.body<Int>()

        downloadService.limitParallelDownload = limit

        return ServerResponse.ok().body(limit)
    }

    fun stopAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        downloadService.stopAllDownload()
        return ServerResponse.noContent().build()
    }

    fun stopOne(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))

        downloadService.removeItemFromQueueAndDownload(id)

        return ServerResponse.noContent().build()
    }

    fun queue(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val queue = downloadService.queue
            .map(::toDownloadingItem)

        val response = QueueItemsHAL(queue)

        return ServerResponse.ok().body(response)
    }


    fun moveInQueue(r: ServerRequest): ServerResponse {
        val form = r.body<MovingItemInQueueForm>()

        downloadService.moveItemInQueue(form.id, form.position)

        return ServerResponse.noContent().build()
    }

    fun removeFromQueue(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))
        val stop = r.paramOrNull("stop")?.toBoolean() ?: false

        downloadService.removeItemFromQueue(id, stop)

        return ServerResponse.noContent().build()
    }
}

private data class DownloadingItemsHAL(val items: List<DownloadingItemHAL>)
private data class QueueItemsHAL(val items: List<DownloadingItemHAL>)

private data class DownloadingItemHAL(
        val id: UUID,
        val title: String,
        val status: Status,
        val progression: Int,
        val podcast: Podcast,
        val cover: Cover
) {
    data class Podcast(val id: UUID, val title: String)
    data class Cover(val id: UUID, val url: URI)
}

private fun toDownloadingItem(item: DownloadingItem): DownloadingItemHAL {
    val extension = item.cover.url.extension().ifBlank { "jpg" }

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString(), "cover.$extension")
            .build(true)
            .toUri()

    return DownloadingItemHAL(
            id = item.id,
            title = item.title,
            status = item.status,
            progression = item.progression,
            podcast = DownloadingItemHAL.Podcast(
                    id = item.podcast.id,
                    title = item.podcast.title
            ),
            cover = DownloadingItemHAL.Cover(
                    id = item.cover.id,
                    url = coverUrl
            )
    )
}

data class MovingItemInQueueForm(val id: UUID, val position: Int)
