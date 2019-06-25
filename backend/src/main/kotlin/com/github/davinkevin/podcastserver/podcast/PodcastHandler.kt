package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.ServerRequest.extractHost
import com.github.davinkevin.podcastserver.item.*
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.seeOther
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-15
 */
@Component
class PodcastHandler(
        private val podcastService: PodcastService,
        private val itemService: ItemService,
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

    fun findAll(r: ServerRequest): Mono<ServerResponse> =
            podcastService.findAll()
                    .map(::toPodcastHAL)
                    .collectList()
                    .map { FindAllPodcastHAL(it) }
                    .flatMap { ok().syncBody(it) }

    fun opml(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        return podcastService.findAll()
                .map { OpmlOutline(it, host)}
                .sort { a,b -> a.title.compareTo(b.title) }
                .collectList()
                .map { Opml(it).toXML() }
                .flatMap { ok().contentType(MediaType.APPLICATION_XML).syncBody(it) }
    }

    fun create(r: ServerRequest): Mono<ServerResponse> = r
            .bodyToMono<PodcastCreationHAL>()
            .map { it.toPodcastCreation() }
            .flatMap { podcastService.save(it) }
            .map(::toPodcastHAL)
            .flatMap { ok().syncBody(it) }

    fun update(r: ServerRequest): Mono<ServerResponse> = r
            .bodyToMono<PodcastUpdateHAL>()
            .map { it.toPodcastUpdate() }
            .flatMap { podcastService.update(it) }
            .map(::toPodcastHAL)
            .flatMap { ok().syncBody(it) }

    fun cover(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        val id = UUID.fromString(r.pathVariable("id"))

        return podcastService.findById(id)
                .doOnNext { log.debug("the url of the podcast cover is ${it.cover.url}")}
                .flatMap { podcast -> podcast
                        .toMono()
                        .flatMap { fileService.coverExists(it) }
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

    fun items(r: ServerRequest): Mono<ServerResponse> {
        val q: String? = r.queryParam("q").filter { it.isNotEmpty() }.orElse(null)
        val page = r.queryParam("page").map { it.toInt() }.orElse(0)
        val size  = r.queryParam("size").map { it.toInt() }.orElse(12)
        val (field, direction) = r.queryParam("sort").orElse("pubDate,DESC").split(",")
        val podcastId = UUID.fromString(r.pathVariable("id"))

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

private data class PodcastCreationHAL(
        val title: String,
        val url: URI,
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
        val url: URI,
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

private class Opml(val podcastOutlines: List<OpmlOutline>) {

    fun toXML(): String {
        val opml = Element("opml").apply {
            setAttribute("version", "2.0")

            val head = Element("head").apply {
                addContent(Element("title").addContent("Podcast-Server"))
            }

            val outlines = podcastOutlines
                    .sortedBy { it.title }
                    .map { it.toXML() }

            val body = Element("body")
                    .addContent(outlines)

            addContent(head)
            addContent(body)
        }

        return XMLOutputter(Format.getPrettyFormat()).outputString(Document(opml))
    }
}

private class OpmlOutline(p: Podcast, host: URI) {
    val text = p.title
    val description = p.description
    val htmlUrl = host.toASCIIString()!! + "podcasts/${p.id}"
    val title = p.title
    val type = "rss"
    val version = "RSS2"
    val xmlUrl = host.toASCIIString()!! + "api/podcasts/${p.id}/rss"

    fun toXML() = Element("outline").apply {
        setAttribute("text", title)
        setAttribute("description", description ?: "")
        setAttribute("htmlUrl", htmlUrl)
        setAttribute("title", title)
        setAttribute("type", "rss")
        setAttribute("version", "RSS2")
        setAttribute("xmlUrl", xmlUrl)
    }
}

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
