package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*

/**
 * Created by kevin on 17/09/2019
 */
class DownloadHandler(private val downloadService: ItemDownloadManager) {

    suspend fun download(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))

        downloadService.addItemToQueue(id)

        return noContent().buildAndAwait()
    }

    suspend fun downloading(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val items = downloadService.downloading.asFlow()
            .map { it.toDownloadingItem() }
            .toList()

        return ok().bodyValueAndAwait(DownloadingItemsHAL(items))
    }

    suspend fun findLimit(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        return ok().bodyValueAndAwait(downloadService.limitParallelDownload)
    }

    suspend fun updateLimit(r: ServerRequest): ServerResponse {
        val limit = r.awaitBody<Int>()

        downloadService.setLimitParallelDownload(limit)

        return ok().bodyValueAndAwait(limit)
    }

    suspend fun stopAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        downloadService.stopAllDownload()
        return noContent().buildAndAwait()
    }

    suspend fun pauseAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        downloadService.pauseAllDownload()
        return noContent().buildAndAwait()
    }

    suspend fun restartAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        downloadService.restartAllDownload()
        return noContent().buildAndAwait()
    }

    suspend fun stopOne(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))
        downloadService.stopDownload(id)
        return noContent().buildAndAwait()
    }

    suspend fun toggleOne(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))
        downloadService.toggleDownload(id)
        return noContent().buildAndAwait()
    }

    suspend fun queue(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val items = downloadService.queue.asFlow()
            .map { it.toDownloadingItem() }
            .toList()

        return ok().bodyValueAndAwait(QueueItemsHAL(items))
    }


    suspend fun moveInQueue(r: ServerRequest): ServerResponse {
        val body = r.awaitBody<MovingItemInQueueForm>()
        downloadService.moveItemInQueue(body.id, body.position)
        return noContent().buildAndAwait()
    }

    suspend fun removeFromQueue(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))
        val stop = r.queryParam("stop").map { it!!.toBoolean() }.orElse(false)

        downloadService.removeItemFromQueue(id, stop)
        return noContent().buildAndAwait()
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

private fun DownloadingItem.toDownloadingItem(): DownloadingItemHAL {
    val extension = cover.url.extension()

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), "cover.$extension")
            .build(true)
            .toUri()

    return DownloadingItemHAL(
            id = id,
            title = title,
            status = status,
            progression = progression,
            podcast = DownloadingItemHAL.Podcast(
                    id = podcast.id,
                    title = podcast.title
            ),
            cover = DownloadingItemHAL.Cover(
                    id = cover.id,
                    url = coverUrl
            )
    )
}

internal data class MovingItemInQueueForm(val id: UUID, val position: Int)
