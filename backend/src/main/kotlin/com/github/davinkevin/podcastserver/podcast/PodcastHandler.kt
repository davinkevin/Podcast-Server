package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.service.storage.FileDescriptor
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

/**
 * Created by kevin on 2019-02-15
 */
class PodcastHandler(
        private val podcastService: PodcastService,
        private val fileService: FileStorageService
) {

    private var log = LoggerFactory.getLogger(PodcastHandler::class.java)

    fun findById(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))

        return podcastService.findById(id)
                .map(::toPodcastHAL)
                .flatMap { ok().bodyValue(it) }
    }

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            podcastService.findAll()
                    .map(::toPodcastHAL)
                    .collectList()
                    .map { FindAllPodcastHAL(it) }
                    .flatMap { ok().bodyValue(it) }

    fun create(r: ServerRequest): Mono<ServerResponse> = r
            .bodyToMono<PodcastCreationHAL>()
            .map { it.toPodcastCreation() }
            .flatMap { podcastService.save(it) }
            .map(::toPodcastHAL)
            .flatMap { ok().bodyValue(it) }

    fun update(r: ServerRequest): Mono<ServerResponse> = r
            .bodyToMono<PodcastUpdateHAL>()
            .map { it.toPodcastUpdate() }
            .flatMap { podcastService.update(it) }
            .map(::toPodcastHAL)
            .flatMap { ok().bodyValue(it) }

    fun delete(r: ServerRequest): Mono<ServerResponse> {
        val podcastId = UUID.fromString(r.pathVariable("id"))

        return podcastService
                .deleteById(podcastId)
                .then(noContent().build())
    }

    fun cover(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        val id = UUID.fromString(r.pathVariable("id"))

        return podcastService.findById(id)
                .doOnNext { log.debug("the url of the podcast cover is ${it.cover.url}")}
                .flatMap { podcast -> podcast
                        .toMono()
                        .flatMap { fileService.coverExists(it) }
//                        .map { UriComponentsBuilder.fromUri(host)
//                                .pathSegment("data", podcast.title, it)
//                                .build().toUri()
//                        }
                        .map { fileService.toExternalUrl(FileDescriptor(podcast.title, it), host) }
                        .switchIfEmpty { podcast.cover.url.toMono() }
                }
                .doOnNext { log.debug("Redirect cover to {}", it)}
                .flatMap { seeOther(it).build() }
    }

    fun findStatByPodcastIdAndPubDate(r: ServerRequest): Mono<ServerResponse> = statsBy(r) { id, number -> podcastService.findStatByPodcastIdAndPubDate(id, number) }
    fun findStatByPodcastIdAndDownloadDate(r: ServerRequest): Mono<ServerResponse> = statsBy(r) { id, number -> podcastService.findStatByPodcastIdAndDownloadDate(id, number) }
    fun findStatByPodcastIdAndCreationDate(r: ServerRequest): Mono<ServerResponse> = statsBy(r) { id, number -> podcastService.findStatByPodcastIdAndCreationDate(id, number) }

    private fun statsBy(r: ServerRequest, proj: (id: UUID, n: Int) -> Flux<NumberOfItemByDateWrapper>): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))
        val numberOfMonths = r.queryParam("numberOfMonths").orElse("1").toInt()

        return proj(id, numberOfMonths)
                .collectList()
                .flatMap { ok().bodyValue(it) }
    }

    fun findStatByTypeAndCreationDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndCreationDate(number) }
    fun findStatByTypeAndPubDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndPubDate(number) }
    fun findStatByTypeAndDownloadDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndDownloadDate(number) }

    private fun statsBy(r: ServerRequest, proj: (n: Int) -> Flux<StatsPodcastType>): Mono<ServerResponse> {
        val numberOfMonths = r.queryParam("numberOfMonths").orElse("1").toInt()

        return proj(numberOfMonths)
                .collectList()
                .flatMap { ok().bodyValue(StatsPodcastTypeWrapperHAL(it)) }
    }
}

private class FindAllPodcastHAL(val content: Collection<PodcastHAL>)
private class StatsPodcastTypeWrapperHAL(val content: Collection<StatsPodcastType>)

private data class PodcastHAL(val id: UUID,
                              val title: String,
                              val url: String?,
                              val hasToBeDeleted: Boolean,
                              val lastUpdate: OffsetDateTime?,
                              val type: String,

                              val tags: Collection<TagHAL>,

                              val cover: CoverHAL)

private data class CoverHAL(val id: UUID, val width: Int, val height: Int, val url: URI)
private data class TagHAL(val id: UUID, val name: String)

private fun Cover.extension(): String {
    return Path(url.path).extension.ifBlank { "jpg" }
}

private fun toPodcastHAL(p: Podcast): PodcastHAL {

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", p.id.toString(), "cover." + p.cover.extension())
            .build(true)
            .toUri()

    return PodcastHAL(
            p.id, p.title, p.url, p.hasToBeDeleted, p.lastUpdate, p.type,
            p.tags.map { TagHAL(it.id, it.name) },
            CoverHAL(p.cover.id, p.cover.width, p.cover.height, coverUrl)
    )
}

private data class PodcastCreationHAL(
        val title: String,
        val url: URI?,
        val tags: Collection<TagForCreationHAL>?,
        val type: String,
        val hasToBeDeleted: Boolean,
        val cover: CoverForCreationHAL
) {
    fun toPodcastCreation() = PodcastForCreation(
            title = title,
            url = url,
            tags = (tags ?: listOf()).map { TagForCreation(it.id, it.name) },
            type = type,
            hasToBeDeleted = hasToBeDeleted,
            cover = CoverForCreation(cover.width, cover.height, cover.url)
    )
}
private data class TagForCreationHAL(val id: UUID?, val name: String)
private data class CoverForCreationHAL(val width: Int, val height: Int, val url: URI)

private data class PodcastUpdateHAL(
        val id: UUID,
        val title: String,
        val url: URI?,
        val hasToBeDeleted: Boolean,
        val tags: Collection<TagForCreationHAL>?,
        val cover: CoverForCreationHAL
) {
    fun toPodcastUpdate() = PodcastForUpdate(
            id = id,
            title = title,
            url = url,
            hasToBeDeleted = hasToBeDeleted,
            tags = (tags ?: listOf()).map { TagForCreation(it.id, it.name) },
            cover = CoverForCreation(cover.width, cover.height, cover.url)
    )
}

