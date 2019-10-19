package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.apache.commons.io.FilenameUtils
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*

/**
 * Created by kevin on 17/09/2019
 */
class DownloadHandler(private val downloadService: ItemDownloadManager) {

    fun download(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))
        downloadService.addItemToQueue(id)
        return noContent().build()
    }

    fun downloading(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            downloadService
                    .downloading
                    .map { toDownloadingItem(it) }
                    .collectList()
                    .map { DownloadingItemsHAL(it) }
                    .flatMap { ok().bodyValue(it) }

    fun findLimit(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            ok().bodyValue(downloadService.limitParallelDownload)

    fun updateLimit(r: ServerRequest): Mono<ServerResponse> =
            r.bodyToMono<Int>()
                    .delayUntil { downloadService.setLimitParallelDownload(it); Mono.empty<Void>() }
                    .flatMap { ok().bodyValue(it) }

    fun stopAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> {
        downloadService.stopAllDownload()
        return noContent().build()
    }

    fun pauseAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> {
        downloadService.pauseAllDownload()
        return noContent().build()
    }

    fun restartAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> {
        downloadService.restartAllDownload()
        return noContent().build()
    }

    fun stopOne(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))
        downloadService.stopDownload(id)
        return noContent().build()
    }

    fun toggleOne(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))
        downloadService.toggleDownload(id)
        return noContent().build()
    }

    fun queue(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            downloadService
                    .queue
                    .map { toDownloadingItem(it) }
                    .collectList()
                    .map { QueueItemsHAL(it) }
                    .flatMap { ok().bodyValue(it) }


    fun moveInQueue(r: ServerRequest): Mono<ServerResponse> =
            r.bodyToMono<MovingItemInQueueForm>()
                    .delayUntil { downloadService.moveItemInQueue(it.id, it.position); Mono.empty<Void>() }
                    .flatMap { noContent().build() }

    fun removeFromQueue(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))
        val stop = r.queryParam("stop").map { it!!.toBoolean() }.orElse(false)

        downloadService.removeItemFromQueue(id, stop)
        return noContent().build()
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
    val extension = (FilenameUtils.getExtension(item.cover.url.toASCIIString()) ?: "jpg")
            .substringBeforeLast("?")

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
