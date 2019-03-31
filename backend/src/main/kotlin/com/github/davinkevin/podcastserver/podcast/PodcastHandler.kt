package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.extension.ServerRequest.extractHost
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.seeOther
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.net.URI
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-15
 */
@Component
class PodcastHandler(
        private val podcastService: PodcastService,
        private val p: PodcastServerParameters,
        private val fileService: FileService
) {

    private var log = LoggerFactory.getLogger(PodcastHandler::class.java)

    fun findById(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))

        return podcastService.findById(id)
                .map(::toPodcastHAL)
                .flatMap { ok().syncBody(it) }
    }

    fun cover(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        val id = UUID.fromString(r.pathVariable("id"))

        return podcastService.findById(id)
                .doOnNext { log.debug("the url of the podcast cover is ${it.cover.url}")}
                .flatMap { podcast -> podcast
                        .toMono()
                        .map { Paths.get(it.title).resolve("${p.coverDefaultName}.${it.cover.extension()}") }
                        .flatMap { fileService.exists(it) }
                        .map { it.toString().substringAfterLast("/") }
                        .map { UriComponentsBuilder.fromUri(host)
                                .pathSegment("data", podcast.title, it)
                                .build().toUri()
                        }
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
                .flatMap { ok().syncBody(it) }
    }


    fun findStatByTypeAndCreationDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndCreationDate(number) }
    fun findStatByTypeAndPubDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndPubDate(number) }
    fun findStatByTypeAndDownloadDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndDownloadDate(number) }

    private fun statsBy(r: ServerRequest, proj: (n: Int) -> Flux<StatsPodcastType>): Mono<ServerResponse> {
        val numberOfMonths = r.queryParam("numberOfMonths").orElse("1").toInt()

        return proj(numberOfMonths)
                .collectList()
                .flatMap { ok().syncBody(StatsPodcastTypeWrapperHAL(it)) }
    }
}

private class StatsPodcastTypeWrapperHAL(val content: Collection<StatsPodcastType>)

private fun CoverForPodcast.extension() = FilenameUtils.getExtension(url.path)

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

private fun toPodcastHAL(p: Podcast): PodcastHAL {

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", p.id.toString(), "cover." + FilenameUtils.getExtension(p.cover.url.path))
            .build(true)
            .toUri()

    return PodcastHAL(
            p.id, p.title, p.url, p.hasToBeDeleted, p.lastUpdate, p.type,
            p.tags.map { TagHAL(it.id, it.name) },
            CoverHAL(p.cover.id, p.cover.width, p.cover.height, coverUrl)
    )
}
