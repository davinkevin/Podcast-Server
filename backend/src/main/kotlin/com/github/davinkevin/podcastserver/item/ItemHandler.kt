package com.github.davinkevin.podcastserver.item

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.*
import com.github.davinkevin.podcastserver.service.storage.FileDescriptor
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import jakarta.servlet.http.Part
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.extension

/**
 * Created by kevin on 2019-02-09
 */
class ItemHandler(
    private val itemService: ItemService,
    private val fileService: FileStorageService,
    private val clock: Clock
) {

    private var log = LoggerFactory.getLogger(ItemHandler::class.java)

    fun clean(r: ServerRequest): ServerResponse {
        val retentionNumberOfDays = r.paramOrNull("days")?.toLong() ?: 30L

        val date = OffsetDateTime.now(clock)
            .minusDays(retentionNumberOfDays)

        itemService.deleteItemOlderThan(date)

        return ServerResponse.ok().build()
    }

    fun reset(s: ServerRequest): ServerResponse {
        val id = s.pathVariable("id").let(UUID::fromString)

        val item = itemService.reset(id)!!

        return ServerResponse.ok().body(item.toHAL())
    }

    fun file(s: ServerRequest): ServerResponse {
        val host = s.extractHost()
        val id = s.pathVariable("id")
            .let(UUID::fromString)

        val item = itemService.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No item found for id $id")

        val uri = findURIOf(item, host)

        log.debug("Redirect content playable to {}", uri)

        return ServerResponse.seeOther(uri).build()
    }

    fun cover(s: ServerRequest): ServerResponse {
        val host = s.extractHost()
        val id = s.pathVariable("id")
            .let(UUID::fromString)

        val item = itemService.findById(id)!!.also {
            log.debug("the url of cover is {}", it.cover.url)
        }
        val coverUrl = findCoverURIOf(item, host)

        log.debug("Redirect cover to {}", coverUrl)

        return ServerResponse.seeOther(coverUrl).build()
    }

    private fun findCoverURIOf(item: Item, host: URI): URI {
        val coverPath = fileService.coverExists(item).block()
            ?: return item.cover.url

        val fileDescriptor = FileDescriptor(item.podcast.title, coverPath)

        return fileService.toExternalUrl(fileDescriptor, host)
    }

    fun findById(s: ServerRequest): ServerResponse {
        val id = s.pathVariable("id")
            .let(UUID::fromString)

        val item = itemService.findById(id)!!
            .toHAL()

        return ServerResponse.ok().body(item)
    }

    fun search(r: ServerRequest): ServerResponse {
        val q: String = r.extractQuery()
        val tags = r.extractTags()
        val status = r.extractStatus()
        val itemPageable = r.toPageRequest()

        val result = itemService.search(
            q = q,
            tags = tags,
            status = status,
            page = itemPageable,
            podcastId = null
        )

        return ServerResponse.ok().body(result.toHAL())
    }

    fun podcastItems(r: ServerRequest): ServerResponse {
        val podcastId = r.pathVariable("idPodcast")
            .let(UUID::fromString)

        val q: String = r.extractQuery()
        val tags = r.extractTags()
        val status = r.extractStatus()
        val itemPageable = r.toPageRequest()

        val items = itemService.search(
            q = q,
            tags = tags,
            status = status,
            page = itemPageable,
            podcastId = podcastId
        )

        return ServerResponse.ok().body(items.toHAL())
    }

    fun upload(r: ServerRequest): ServerResponse {
        val podcastId = UUID.fromString(r.pathVariable("idPodcast")).also {
            log.debug("uploading to podcast {}", it)
        }
        val host = r.extractHost()

        val parts = r.multipartData().toSingleValueMap()
        val file = parts["file"]
            ?.let(::InternalFilePart)
            ?.also { log.info("upload of file ${it.filename()}") }
            ?: error("`file` field not available in upload request")

        val item = itemService.upload(podcastId, file)
            .toHAL()

        return ServerResponse
            .created(URI("${host}api/v1/items/${item.id}"))
            .body(item)
    }

    fun playlists(r: ServerRequest): ServerResponse {
        val itemId = r.pathVariable("id")
            .let(UUID::fromString)

        val response = itemService
            .findPlaylistsContainingItem(itemId)
            .map { PlaylistsHAL.PlaylistHAL(it.id, it.name) }
            .let(::PlaylistsHAL)

        return ServerResponse.ok().body(response)
    }

    fun delete(r: ServerRequest): ServerResponse {
        val itemId = r.pathVariable("id").let(UUID::fromString)

        itemService.deleteById(itemId)

        return ServerResponse.ok().build()
    }

    private fun findURIOf(item: Item, host: URI): URI {
        if (!item.isDownloaded()) return URI.create(item.url!!)

        val descriptor = FileDescriptor(item.podcast.title, item.fileName!!)

        return fileService.toExternalUrl(descriptor, host)
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

data class InternalFilePart(val part: Part): FilePart {

    override fun name(): String = part.submittedFileName

    override fun headers(): HttpHeaders {
        TODO("Not yet implemented")
    }

    override fun content(): Flux<DataBuffer> {
        TODO("Not yet implemented")
    }

    override fun filename(): String = part.submittedFileName

    override fun transferTo(dest: Path): Mono<Void> = Mono.defer {
        Files.copy(part.inputStream, dest)
            .toMono()
            .then()
    }
        .subscribeOn(Schedulers.parallel())
}