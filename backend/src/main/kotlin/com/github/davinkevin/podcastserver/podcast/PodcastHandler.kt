package com.github.davinkevin.podcastserver.podcast

import arrow.core.Option
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.extension.ServerRequest.extractHost
import com.github.davinkevin.podcastserver.item.*
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
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
import reactor.core.publisher.*
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import reactor.util.function.component1
import reactor.util.function.component2

/**
 * Created by kevin on 2019-02-15
 */
@Component
class PodcastHandler(
        private val podcastService: PodcastService,
        private val itemService: ItemService,
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

    fun rss(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        val podcastId = UUID.fromString(r.pathVariable("id"))
        val limit  = r.queryParam("limit")
                .map { it!!.toBoolean() }
                .orElse(true)
        val limitNumber = if (limit) 50 else Int.MAX_VALUE

        val itemPageable = ItemPageRequest(0, limitNumber, ItemSort("DESC", "pubDate"))

        val items = itemService.search(
                q = null,
                tags = listOf(),
                statuses = listOf(),
                page = itemPageable,
                podcastId = podcastId
        )
                .flatMapMany { it.content.toFlux() }
                .map { toRssItem(it, host) }
                .collectList()

        val podcast = podcastService
                .findById(podcastId)
                .map { toRssChannel(it, host) }


        return Mono
                .zip(items, podcast)
                .map { (itemRss, podcastRss) -> podcastRss.addContent(itemRss) }
                .map {
                    val rss = Element("rss").apply {
                        addContent(it)
                        addNamespaceDeclaration(itunesNS)
                        addNamespaceDeclaration(mediaNS)
                    }

                    XMLOutputter(Format.getPrettyFormat()).outputString(Document(rss))
                }
                .flatMap { ok().contentType(MediaType.APPLICATION_XML).syncBody(it) }
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

private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")
private val mediaNS = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")

private fun toRssItem(item: Item, host: URI): Element {

    val coverExtension = (FilenameUtils.getExtension(item.cover.url) ?: "jpg").substringBeforeLast("?")
    val coverUrl = UriComponentsBuilder.fromUri(host)
            .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString(), "cover.$coverExtension")
            .build(true)
            .toUriString()

    val itunesItemThumbnail = Element("image", itunesNS).setContent(Text(coverUrl))
    val thumbnail = Element("thumbnail", mediaNS).setAttribute("url", coverUrl)

    val extension = Option.fromNullable(item.fileName)
            .map { FilenameUtils.getExtension(it) }
            .map { it.substringBeforeLast("?") }
            .map { ".$it" }
            .getOrElse { "" }

    val title = item.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + extension

    val proxyURL = UriComponentsBuilder
            .fromUri(host)
            .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString(), title)
            .build(true)
            .toUriString()

    val itemEnclosure = Element("enclosure").apply {
        setAttribute("url", proxyURL)

        if(item.length != null) {
            setAttribute("length", item.length.toString())
        }

        if(item.mimeType?.isNotEmpty() == true) {
            setAttribute("type", item.mimeType)
        }
    }

    return Element("item").apply {
        addContent(Element("title").addContent(Text(item.title)))
        addContent(Element("description").addContent(Text(item.description)))
        addContent(Element("pubDate").addContent(Text(item.pubDate!!.format(DateTimeFormatter.RFC_1123_DATE_TIME))))
        addContent(Element("explicit", itunesNS).addContent(Text("No")))
        addContent(Element("subtitle", itunesNS).addContent(Text(item.title)))
        addContent(Element("summary", itunesNS).addContent(Text(item.description)))
        addContent(Element("guid").addContent(Text(proxyURL)))
        addContent(itunesItemThumbnail)
        addContent(thumbnail)
        addContent(itemEnclosure)
    }

}

private fun toRssChannel(podcast: Podcast, host: URI): Element {
    val url = UriComponentsBuilder
            .fromUri(host)
            .pathSegment("api", "v1", "podcasts", podcast.id.toString())
            .build(true)
            .toUriString()

    val coverUrl = UriComponentsBuilder.fromUri(host)
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "cover." + FilenameUtils.getExtension(podcast.cover.url.path))
            .build(true)
            .toUriString()

    return Element("channel").apply {
        addContent(Element("title").addContent(Text(podcast.title)))
        addContent(Element("link").addContent(Text(url)))
        addContent(Element("description").addContent(Text(podcast.description)))
        addContent(Element("subtitle", itunesNS).addContent(Text(podcast.description)))
        addContent(Element("summary", itunesNS).addContent(Text(podcast.description)))
        addContent(Element("language").addContent(Text("fr-fr")))
        addContent(Element("author", itunesNS).addContent(Text(podcast.type)))
        addContent(Element("category", itunesNS))

        if (podcast.lastUpdate != null) {
            val d = podcast.lastUpdate.format(DateTimeFormatter.RFC_1123_DATE_TIME)
            addContent(Element("pubDate").addContent(d))
        }

        val itunesImage = Element("image", JdomService.ITUNES_NAMESPACE).apply { addContent(Text(coverUrl)) }

        val image = Element("image").apply {
            addContent(Element("height").addContent(podcast.cover.height.toString()))
            addContent(Element("url").addContent(coverUrl))
            addContent(Element("width").addContent(podcast.cover.width.toString()))
        }

        addContent(image)
        addContent(itunesImage)
    }
}
