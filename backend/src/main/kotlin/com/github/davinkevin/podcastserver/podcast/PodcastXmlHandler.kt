package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.extension.serverRequest.normalizedURI
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.ItemPageRequest
import com.github.davinkevin.podcastserver.item.ItemService
import com.github.davinkevin.podcastserver.item.ItemSort
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
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


private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")
private val mediaNS = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
private val ITUNES_NAMESPACE = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")

private fun toRssItem(item: Item, host: URI): Element {

    val podcastUrlBuilder = UriComponentsBuilder.fromUri(host)
        .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString())

    val coverExtension = (Path(item.cover.url.path).extension.ifBlank { "jpg" })
    val coverUrl = podcastUrlBuilder
        .cloneBuilder()
        .pathSegment("cover.$coverExtension")
        .build(true)
        .toUriString()

    val itunesItemThumbnail = Element("image", itunesNS).setContent(Text(coverUrl))
    val thumbnail = Element("thumbnail", mediaNS).setAttribute("url", coverUrl)

    val proxyURL = podcastUrlBuilder
        .cloneBuilder()
        .pathSegment(item.slug())
        .build(true)
        .toUriString()

    val itemEnclosure = Element("enclosure").apply {
        setAttribute("url", proxyURL)

        if(item.length != null) {
            setAttribute("length", item.length.toString())
        }

        if(item.mimeType.isNotEmpty()) {
            setAttribute("type", item.mimeType)
        }
    }

    val guid = Element("guid").apply {
        val uuidUrl = UriComponentsBuilder
            .fromUri(host)
            .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString())
            .build(true)
            .toUriString()

        addContent(Text(uuidUrl))
    }

    return Element("item").apply {
        addContent(Element("title").addContent(Text(item.title)))
        addContent(Element("description").addContent(Text(item.description)))
        addContent(Element("pubDate").addContent(Text(item.pubDate!!.format(DateTimeFormatter.RFC_1123_DATE_TIME))))
        addContent(Element("explicit", itunesNS).addContent(Text("No")))
        addContent(Element("subtitle", itunesNS).addContent(Text(item.title)))
        addContent(Element("summary", itunesNS).addContent(Text(item.description)))
        addContent(guid)
        addContent(itunesItemThumbnail)
        addContent(thumbnail)
        addContent(itemEnclosure)
    }

}

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

        val itunesImage = Element("image", ITUNES_NAMESPACE).apply { addContent(Text(coverUrl)) }

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
