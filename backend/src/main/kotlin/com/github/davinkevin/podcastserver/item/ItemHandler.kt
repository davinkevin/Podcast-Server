package com.github.davinkevin.podcastserver.item

import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.ServerRequest.extractHost
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import io.vavr.API
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.seeOther
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.net.URI
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemHandler(val itemService: ItemService, val fileService: FileService) {

    private var log = LoggerFactory.getLogger(ItemHandler::class.java)

    fun clean(s: ServerRequest) =
            itemService
                    .deleteOldEpisodes()
                    .then(ok().build())

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
    }

    fun cover(s: ServerRequest): Mono<ServerResponse> {
        val host = s.extractHost()
        val id = UUID.fromString(s.pathVariable("id"))
        return itemService.findById(id)
                .doOnNext { log.debug("the url of cover is ${it.cover.url}")}
                .flatMap { item -> item
                        .toMono()
                        .map { Paths.get(it.podcast.title).resolve("${it.id}.${it.cover.extension()}") }
                        .flatMap { fileService.exists(it) }
                        .map { it.toString().substringAfterLast("/") }
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
}

private fun CoverForItem.extension() = FilenameUtils.getExtension(url) ?: "jpg"

data class ItemHAL(
        val id: UUID, val title: String, val url: String,
        val pubDate: OffsetDateTime?, val downloadDate: OffsetDateTime?, val creationDate: OffsetDateTime?,
        val description: String?, val mimeType: String?, val length: Long?, val fileName: String?, val status: Status,
        val podcast: PodcastHAL, val cover: CoverHAL
) {
    val podcastId = podcast.id

    val proxyURL: URI
        get() {
            val extension = Option.fromNullable(fileName)
                    .map { FilenameUtils.getExtension(it) }
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

data class CoverHAL(val id: UUID, val url: String, val width: Int, val height: Int)
data class PodcastHAL(val id: UUID, val title: String, val url: String)

fun toItemHAL(i: Item) = ItemHAL(
        id = i.id, title = i.title, url = i.url,
        pubDate = i.pubDate, downloadDate = i.downloadDate, creationDate = i.creationDate,
        description = i.description, mimeType = i.mimeType, length = i.length, fileName = i.fileName, status = i.status,

        podcast = PodcastHAL(i.podcast.id, i.podcast.title, i.podcast.url),
        cover = CoverHAL(i.cover.id, i.cover.url, i.cover.width, i.cover.height)
)
