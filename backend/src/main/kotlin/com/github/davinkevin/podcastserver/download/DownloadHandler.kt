package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
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
                    .flatMap { ok().syncBody(it) }

    fun findLimit(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            ok().syncBody(downloadService.limitParallelDownload)

    fun updateLimit(r: ServerRequest): Mono<ServerResponse> =
            r.bodyToMono<Int>()
                    .delayUntil { downloadService.setLimitParallelDownload(it); Mono.empty<Void>() }
                    .flatMap { ok().syncBody(it) }

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
                    .flatMap { ok().syncBody(it) }


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
        val podcast: Podcast,
        val cover: Cover
) {
    data class Podcast(val id: UUID, val title: String)
    data class Cover(val id: UUID, val url: URI)
}

private fun toDownloadingItem(item: Item): DownloadingItemHAL {
    val extension = (FilenameUtils.getExtension(item.cover?.url) ?: "jpg")
            .substringBeforeLast("?")

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", item.podcast?.id.toString(), "items", item.id.toString(), "cover.$extension")
            .build(true)
            .toUri()

    return DownloadingItemHAL(
            id = item.id ?: error("item id is not defined"),
            title = item.title ?: error("item title is not defined"),
            status = item.status,
            podcast = DownloadingItemHAL.Podcast(
                    id = item.podcast?.id ?: error("podcast id not defined"),
                    title = item.podcast?.title ?: error("podcast title not defined")
            ),
            cover = DownloadingItemHAL.Cover(
                    id = item.cover?.id ?: error("cover id not defined"),
                    url = coverUrl
            )
    )
}

data class MovingItemInQueueForm(val id: UUID, val position: Int)
