package com.github.davinkevin.podcastserver.item

import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.service.FileService
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemHandler(val itemService: ItemService, val fileService: FileService) {

    private var log = LoggerFactory.getLogger(ItemHandler::class.java)

    fun clean(@Suppress("UNUSED_PARAMETER") s: ServerRequest) =
            itemService
                    .deleteOldEpisodes()
                    .then(ok().build())

    fun reset(s: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.reset(id)
                .map(::toItemHAL)
                .flatMap { ok().syncBody(it) }
    }

    fun file(s: ServerRequest): Mono<ServerResponse> {
        val host = s.extractHost()
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.findById(id)
                .flatMap { item -> item
                            .toMono()
                            .filter(Item::isDownloaded)
                            .map { UriComponentsBuilder.fromUri(host)
                                    .pathSegment("data", it.podcast.title, it.fileName)
                                    .build().toUri() }
                            .switchIfEmpty { URI(item.url).toMono() }
                }
                .doOnNext { log.debug("Redirect content playabe to {}", it)}
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
                        .map { UriComponentsBuilder.fromUri(host)
                                    .pathSegment("data", item.podcast.title, it)
                                    .build().toUri()
                        }
                        .switchIfEmpty { URI(item.cover.url).toMono() }
                }
                .doOnNext { log.debug("Redirect cover to {}", it)}
                .flatMap { seeOther(it).build() }
    }

    fun findById(s: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.findById(id)
                .map(::toItemHAL)
                .flatMap { ok().syncBody(it) }
    }

    fun search(s: ServerRequest): Mono<ServerResponse> {
        val q: String? = s.queryParam("q").filter { it.isNotEmpty() }.orElse(null)
        val page = s.queryParam("page").map { it.toInt() }.orElse(0)
        val size  = s.queryParam("size").map { it.toInt() }.orElse(12)
        val sort  = s.queryParam("sort").orElse("pubDate,DESC")
        val field = sort.split(",").first()
        val direction = sort.split(",").last()

        val itemPageable = ItemPageRequest(page, size, ItemSort(direction, field))

        val tags = s.queryParam("tags")
                .filter { !it.isEmpty() }
                .map { it.split(",") }
                .orElse(listOf())

        val statuses = s.queryParam("status")
                .filter { !it.isEmpty() }
                .map { it.split(",") }
                .orElse(listOf())
                .map { Status.of(it) }

        return itemService.search(
                q = q,
                tags = tags,
                statuses = statuses,
                page = itemPageable,
                podcastId = null
        )
                .map(::toPageItemHAL)
                .flatMap { ok().syncBody(it) }

    }

    fun pocastItems(r: ServerRequest): Mono<ServerResponse> {
        val q: String? = r.queryParam("q").filter { it.isNotEmpty() }.orElse(null)
        val page = r.queryParam("page").map { it.toInt() }.orElse(0)
        val size  = r.queryParam("size").map { it.toInt() }.orElse(12)
        val (field, direction) = r.queryParam("sort").orElse("pubDate,DESC").split(",")
        val podcastId = UUID.fromString(r.pathVariable("idPodcast"))

        val itemPageable = ItemPageRequest(page, size, ItemSort(direction, field))

        val tags = r.queryParam("tags")
                .filter { it.isNotEmpty() }
                .map { it.split(",") }
                .orElse(listOf())

        val statuses = r.queryParam("status")
                .filter { it.isNotEmpty() }
                .map { it.split(",") }
                .orElse(listOf())
                .map { Status.of(it) }

        return itemService.search(
                q = q,
                tags = tags,
                statuses = statuses,
                page = itemPageable,
                podcastId = podcastId
        )
                .map(::toPageItemHAL)
                .flatMap { ok().syncBody(it) }
    }

    fun upload(r: ServerRequest): Mono<ServerResponse> {
        val podcastId = UUID.fromString(r.pathVariable("idPodcast"))
        val host = r.extractHost()
        log.debug("uploading to podcast {}", podcastId)

        return r.body(BodyExtractors.toMultipartData())
                .map { it.toSingleValueMap() }
                .map { it["file"] as FilePart }
                .doOnNext { log.info("upload of file ${it.filename()}") }
                .flatMap { itemService.upload(podcastId, it) }
                .flatMap { created(URI("${host}api/v1/items/${it.id}")).syncBody(it) }
    }

}

private fun CoverForItem.extension() = FilenameUtils.getExtension(url) ?: "jpg"

data class ItemHAL(
        val id: UUID, val title: String, val url: String?,
        val pubDate: OffsetDateTime?, val downloadDate: OffsetDateTime?, val creationDate: OffsetDateTime?,
        val description: String?, val mimeType: String?, val length: Long?, val fileName: String?, val status: Status,
        val podcast: PodcastHAL, val cover: CoverHAL
) {
    val podcastId = podcast.id

    val proxyURL: URI
        get() {
            val extension = Option.fromNullable(fileName)
                    .map { FilenameUtils.getExtension(it) }
                    .map { it.substringBeforeLast("?") }
                    .map { ".$it" }
                    .getOrElse { "" }

            val title = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + extension

            return UriComponentsBuilder.fromPath("/")
                    .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), title)
                    .build(true)
                    .toUri()
        }

    @JsonProperty("isDownloaded")
    private val isDownloaded = Status.FINISH == status
}

data class CoverHAL(val id: UUID, val width: Int, val height: Int, val url: URI)
data class PodcastHAL(val id: UUID, val title: String, val url: String?)

fun toItemHAL(i: Item): ItemHAL {

    val extension = i.cover
            .extension()
            .substringBeforeLast("?")

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", i.podcast.id.toString(), "items", i.id.toString(), "cover.$extension")
            .build(true)
            .toUri()

    return ItemHAL(
            id = i.id, title = i.title, url = i.url,
            pubDate = i.pubDate, downloadDate = i.downloadDate, creationDate = i.creationDate,
            description = i.description, mimeType = i.mimeType, length = i.length, fileName = i.fileName, status = i.status,

            podcast = PodcastHAL(i.podcast.id, i.podcast.title, i.podcast.url),
            cover = CoverHAL(i.cover.id, i.cover.width, i.cover.height, coverUrl)
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

private fun toPageItemHAL(p: PageItem) = PageItemHAL(
        content = p.content.map(::toItemHAL),
        empty = p.empty,
        first = p.first,
        last = p.last,
        number = p.number,
        numberOfElements = p.numberOfElements,
        size = p.size,
        totalElements = p.totalElements,
        totalPages = p.totalPages
)
