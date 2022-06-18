package com.github.davinkevin.podcastserver.item

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.service.storage.FileDescriptor
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.extension

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemHandler(
    private val itemService: ItemService,
    private val fileService: FileStorageService,
    private val clock: Clock
) {

    private var log = LoggerFactory.getLogger(ItemHandler::class.java)

    fun clean(r: ServerRequest): Mono<ServerResponse> {
        val retentionNumberOfDays = r.queryParam("days")
                .map { it.toLong() }
                .orElse(30L)

        val date = OffsetDateTime.now(clock)
                .minusDays(retentionNumberOfDays)

        return itemService
                .deleteItemOlderThan(date)
                .then(ok().build())
    }

    fun reset(s: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.reset(id)
                .map { it.toHAL() }
                .flatMap { ok().bodyValue(it) }
    }

    fun file(s: ServerRequest): Mono<ServerResponse> {
        val host = s.extractHost()
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.findById(id)
                .flatMap { item -> item
                        .toMono()
                        .filter { it.isDownloaded() }
                        .map { fileService.toExternalUrl(FileDescriptor(it.podcast.title, it.fileName!!), host) }
                        .switchIfEmpty { URI(item.url!!).toMono() }
                }
                .doOnNext { log.debug("Redirect content playable to {}", it)}
                .flatMap { seeOther(it).build() }
                .switchIfEmpty { ResponseStatusException(NOT_FOUND, "No item found for id $id").toMono() }
    }

    fun cover(s: ServerRequest): Mono<ServerResponse> {
        val host = s.extractHost()
        val id = UUID.fromString(s.pathVariable("id"))
        return itemService.findById(id)
                .doOnNext { log.debug("the url of cover is ${it.cover.url}")}
                .flatMap { item -> item
                        .toMono()
                        .flatMap { fileService.coverExists(it) }
                        .map { FileDescriptor(item.podcast.title, it) }
                        .map { fileService.toExternalUrl(it, host) }
                        .switchIfEmpty { item.cover.url.toMono() }
                }
                .doOnNext { log.debug("Redirect cover to {}", it)}
                .flatMap { seeOther(it).build() }
    }

    fun findById(s: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.findById(id)
                .map { it.toHAL() }
                .flatMap { ok().bodyValue(it) }
    }

    fun search(r: ServerRequest): Mono<ServerResponse> {
        val q: String = r.extractQuery()
        val tags = r.extractTags()
        val status = r.extractStatus()
        val itemPageable = r.toPageRequest()

        return itemService.search(
                q = q,
                tags = tags,
                status = status,
                page = itemPageable,
                podcastId = null
        )
                .map { it.toHAL() }
                .flatMap { ok().bodyValue(it) }
    }

    fun podcastItems(r: ServerRequest): Mono<ServerResponse> {
        val podcastId = UUID.fromString(r.pathVariable("idPodcast"))

        val q: String = r.extractQuery()
        val tags = r.extractTags()
        val status = r.extractStatus()
        val itemPageable = r.toPageRequest()

        return itemService.search(
                q = q,
                tags = tags,
                status = status,
                page = itemPageable,
                podcastId = podcastId
        )
                .map { it.toHAL() }
                .flatMap { ok().bodyValue(it) }
    }

    private fun ServerRequest.toPageRequest(): ItemPageRequest {
        val page = queryParam("page").map { it.toInt() }.orElse(0)
        val size  = queryParam("size").map { it.toInt() }.orElse(12)
        val (field, direction) = queryParam("sort").orElse("pubDate,DESC").split(",")

        return ItemPageRequest(page, size, ItemSort(direction, field))
    }

    private fun ServerRequest.extractTags(): List<String> = queryParam("tags")
            .filter { it.isNotEmpty() }
            .map { it.split(",") }
            .orElse(listOf())
            .filter { it.isNotEmpty() }

    private fun ServerRequest.extractStatus(): List<Status> = queryParam("status")
            .filter { it.isNotEmpty() }
            .map { it.split(",") }
            .orElse(listOf())
            .filter { it.isNotEmpty() }
            .map { Status.of(it) }

    private fun ServerRequest.extractQuery(): String = queryParam("q").orElse("")


    fun upload(r: ServerRequest): Mono<ServerResponse> {
        val podcastId = UUID.fromString(r.pathVariable("idPodcast"))
        val host = r.extractHost()
        log.debug("uploading to podcast {}", podcastId)


        return r.body(BodyExtractors.toMultipartData())
                .map { it.toSingleValueMap() }
                .map { it["file"] as FilePart }
                .doOnNext { log.info("upload of file ${it.filename()}") }
                .flatMap { itemService.upload(podcastId, it) }
                .map { it.toHAL() }
                .flatMap { created(URI("${host}api/v1/items/${it.id}")).bodyValue(it) }
    }

    fun playlists(r: ServerRequest): Mono<ServerResponse> {
        val itemId = UUID.fromString(r.pathVariable("id"))

        return itemService
                .findPlaylistsContainingItem(itemId)
                .map { PlaylistsHAL.PlaylistHAL(it.id, it.name) }
                .collectList()
                .map { PlaylistsHAL(it) }
                .flatMap { ok().bodyValue(it) }
    }

    fun delete(r: ServerRequest): Mono<ServerResponse> {
        val itemId = UUID.fromString(r.pathVariable("id"))

        return itemService
                .deleteById(itemId)
                .then(ok().build())

    }
}

data class ItemHAL(
    val id: UUID, val title: String, val url: String?,
    val pubDate: OffsetDateTime?, val downloadDate: OffsetDateTime?, val creationDate: OffsetDateTime?,
    val description: String?, val mimeType: String, val length: Long?, val fileName: Path?, val status: Status,
    val podcast: Podcast, val cover: Cover
) {
    val podcastId = podcast.id

    val proxyURL: URI
        get() {
            val extension = fileName?.extension?.let { ".$it" } ?: ""
            val title = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + extension

            return UriComponentsBuilder.fromPath("/")
                    .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), title)
                    .build(true)
                    .toUri()
        }

    @JsonProperty("isDownloaded")
    private val isDownloaded = Status.FINISH == status

    data class Cover(val id: UUID, val width: Int, val height: Int, val url: URI)
    data class Podcast(val id: UUID, val title: String, val url: String?)
}


private fun Item.toHAL(): ItemHAL {

    val extension = cover.url.extension().ifBlank { "jpg" }

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), "cover.$extension")
            .build(true)
            .toUri()

    return ItemHAL(
            id = id, title = title, url = url,
            pubDate = pubDate, downloadDate = downloadDate, creationDate = creationDate,
            description = description, mimeType = mimeType, length = length, fileName = fileName, status = status,

            podcast = ItemHAL.Podcast(podcast.id, podcast.title, podcast.url),
            cover = ItemHAL.Cover(cover.id, cover.width, cover.height, coverUrl)
    )
}

data class PageItemHAL (
        val content: Collection<ItemHAL>,
        val empty: Boolean,
        val first: Boolean,
        val last: Boolean,
        val number: Int,
        val numberOfElements: Int,
        val size: Int,
        val totalElements: Int,
        val totalPages: Int
)

private fun PageItem.toHAL() = PageItemHAL(
        content = content.map { it.toHAL() },
        empty = empty,
        first = first,
        last = last,
        number = number,
        numberOfElements = numberOfElements,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages
)

data class PlaylistsHAL(val content: Collection<PlaylistHAL>) {
    data class PlaylistHAL(val id: UUID, val name: String)
}
