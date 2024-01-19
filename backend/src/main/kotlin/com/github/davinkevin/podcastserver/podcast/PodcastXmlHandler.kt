package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.extension.serverRequest.normalizedURI
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.ItemPageRequest
import com.github.davinkevin.podcastserver.item.ItemService
import com.github.davinkevin.podcastserver.item.ItemSort
import com.github.davinkevin.podcastserver.rss.Item as RssItem
import com.github.davinkevin.podcastserver.rss.itunesNS
import com.github.davinkevin.podcastserver.rss.mediaNS
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Text
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.queryParamOrNull
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

class PodcastXmlHandler(
    private val podcastService: PodcastService,
    private val itemService: ItemService
) {

    fun opml(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        return podcastService.findAll()
            .map { OpmlOutline(it, host)}
            .sort { a,b -> a.title.compareTo(b.title) }
            .collectList()
            .map { Opml(it).toXML() }
            .flatMap { ServerResponse.ok().contentType(MediaType.APPLICATION_XML).bodyValue(it) }
    }

    fun rss(r: ServerRequest): Mono<ServerResponse> {
        val baseUrl = r.extractHost()
        val callUrl = r.normalizedURI()
        val podcastId = UUID.fromString(r.pathVariable("id"))

        val limit = r.queryParamOrNull("limit").toLimit()
        val itemPageable = ItemPageRequest(0, limit, ItemSort("DESC", "pubDate"))

        val items = itemService.search(
            q = "",
            tags = listOf(),
            status = listOf(),
            page = itemPageable,
            podcastId = podcastId
        )
            .flatMapMany { it.content.toFlux() }
            .map { toRssItem(it, baseUrl) }
            .collectList()

        val podcast = podcastService
            .findById(podcastId)
            .map { toRssChannel(it, baseUrl, callUrl) }

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
            .flatMap { ServerResponse.ok().contentType(MediaType.APPLICATION_XML).bodyValue(it) }
    }


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

    val title = p.title
    val description = p.description
    val htmlUrl = host.toASCIIString()!! + "podcasts/${p.id}"
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

private fun toRssItem(item: Item, host: URI): Element = RssItem(
    host = host,
    podcast = RssItem.Podcast(item.podcast.id),
    item = RssItem.Item(
        id = item.id,
        title = item.title,
        mimeType = item.mimeType,
        fileName = item.fileName,
        pubDate = item.pubDate,
        description = item.description,
        length = item.length,
    ),
    cover = RssItem.Cover(
        url = item.cover.url
    )
)
    .toElement()

private fun toRssChannel(podcast: Podcast, baseUrl: URI, callUrl: URI): Element {
    val url = UriComponentsBuilder.fromUri(callUrl).build(true).toUriString()
    val coverUrl = UriComponentsBuilder.fromUri(baseUrl)
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "cover." + Path(podcast.cover.url.path).extension)
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

        val itunesImage = Element("image", itunesNS).apply { addContent(Text(coverUrl)) }

        val image = Element("image").apply {
            addContent(Element("height").addContent(podcast.cover.height.toString()))
            addContent(Element("url").addContent(coverUrl))
            addContent(Element("width").addContent(podcast.cover.width.toString()))
        }

        addContent(image)
        addContent(itunesImage)
    }
}

private fun String?.toLimit(): Int {
    val limitAsBooleanOrNull = this?.toBooleanStrictOrNull()
        ?: return this?.toIntOrNull()
            ?: 50

    if (limitAsBooleanOrNull) {
        return 50
    }

    return Int.MAX_VALUE
}
