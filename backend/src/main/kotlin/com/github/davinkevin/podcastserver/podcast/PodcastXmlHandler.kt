package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.ItemPageRequest
import com.github.davinkevin.podcastserver.item.ItemService
import com.github.davinkevin.podcastserver.item.ItemSort
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.apache.commons.io.FilenameUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.*

@FlowPreview
class PodcastXmlHandler(
    private val podcastService: PodcastService,
    private val itemService: ItemService
) {

    suspend fun opml(r: ServerRequest): ServerResponse {
        val host = r.extractHost()

        val podcasts = podcastService.findAll()
            .asFlow()
            .map { OpmlOutline(it, host)}
            .toList()
            .sortedBy { it.title }

        return ok()
            .contentType(MediaType.APPLICATION_XML)
            .bodyValueAndAwait(Opml(podcasts).toXML())
    }

    suspend fun rss(r: ServerRequest): ServerResponse {
        val host = r.extractHost()
        val podcastId = UUID.fromString(r.pathVariable("id"))
        val limit  = r.queryParam("limit")
            .map { it.toBoolean() }
            .orElse(true)

        val itemPageable = ItemPageRequest(
            page = 0,
            size = if (limit) 50 else Int.MAX_VALUE,
            sort = ItemSort("DESC", "pubDate")
        )

        val podcastRss = coroutineScope {
            val itemsDeferred = async { itemService.search(
                q = "",
                tags = listOf(),
                status = listOf(),
                page = itemPageable,
                podcastId = podcastId
            )
                .asFlow()
                .flatMapConcat { it.content.asFlow() }
                .map { it.toRssItem(host) }
                .toList()
            }

            val podcastDeferred = async {
                val p = podcastService.findById(podcastId).awaitFirst()
                p.toRssChannel(host)
            }

            podcastDeferred.await()
                .addContent(itemsDeferred.await())
        }

        val rss = Element("rss").apply {
            addContent(podcastRss)
            addNamespaceDeclaration(itunesNS)
            addNamespaceDeclaration(mediaNS)
        }

        val body = XMLOutputter(Format.getPrettyFormat()).outputString(Document(rss))

        return ok().contentType(MediaType.APPLICATION_XML).bodyValueAndAwait(body)
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
private val ITUNES_NAMESPACE = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")

private fun Item.toRssItem(host: URI): Element {

    val coverExtension = (FilenameUtils.getExtension(cover.url.toASCIIString()) ?: "jpg").substringBeforeLast("?")
    val coverUrl = UriComponentsBuilder.fromUri(host)
        .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), "cover.$coverExtension")
        .build(true)
        .toUriString()

    val itunesItemThumbnail = Element("image", itunesNS).setContent(Text(coverUrl))
    val thumbnail = Element("thumbnail", mediaNS).setAttribute("url", coverUrl)

    val extension = Optional.ofNullable(fileName)
        .map { FilenameUtils.getExtension(it) }
        .map { it.substringBeforeLast("?") }
        .or { Optional.ofNullable(mimeType).map { it.substringAfter("/") } }
        .map { ".$it" }
        .orElse("")

    val title = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + extension

    val proxyURL = UriComponentsBuilder
        .fromUri(host)
        .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), title)
        .build(true)
        .toUriString()

    val itemEnclosure = Element("enclosure").apply {
        setAttribute("url", proxyURL)

        if(length != null) {
            setAttribute("length", length.toString())
        }

        if(mimeType.isNotEmpty()) {
            setAttribute("type", mimeType)
        }
    }

    val guid = Element("guid").apply {
        val uuidUrl = UriComponentsBuilder
            .fromUri(host)
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString())
            .build(true)
            .toUriString()

        addContent(Text(uuidUrl))
    }

    return Element("item").apply {
        addContent(Element("title").addContent(Text(this@toRssItem.title)))
        addContent(Element("description").addContent(Text(description)))
        addContent(Element("pubDate").addContent(Text(pubDate!!.format(DateTimeFormatter.RFC_1123_DATE_TIME))))
        addContent(Element("explicit", itunesNS).addContent(Text("No")))
        addContent(Element("subtitle", itunesNS).addContent(Text(this@toRssItem.title)))
        addContent(Element("summary", itunesNS).addContent(Text(description)))
        addContent(guid)
        addContent(itunesItemThumbnail)
        addContent(thumbnail)
        addContent(itemEnclosure)
    }

}

private fun Podcast.toRssChannel(host: URI): Element {
    val url = UriComponentsBuilder
        .fromUri(host)
        .pathSegment("api", "v1", "podcasts", id.toString())
        .build(true)
        .toUriString()

    val coverUrl = UriComponentsBuilder.fromUri(host)
        .pathSegment("api", "v1", "podcasts", id.toString(), "cover." + FilenameUtils.getExtension(cover.url.path))
        .build(true)
        .toUriString()

    return Element("channel").apply {
        addContent(Element("title").addContent(Text(title)))
        addContent(Element("link").addContent(Text(url)))
        addContent(Element("description").addContent(Text(description)))
        addContent(Element("subtitle", itunesNS).addContent(Text(description)))
        addContent(Element("summary", itunesNS).addContent(Text(description)))
        addContent(Element("language").addContent(Text("fr-fr")))
        addContent(Element("author", itunesNS).addContent(Text(type)))
        addContent(Element("category", itunesNS))

        if (lastUpdate != null) {
            val d = lastUpdate.format(DateTimeFormatter.RFC_1123_DATE_TIME)
            addContent(Element("pubDate").addContent(d))
        }

        val itunesImage = Element("image", ITUNES_NAMESPACE).apply { addContent(Text(coverUrl)) }

        val image = Element("image").apply {
            addContent(Element("height").addContent(cover.height.toString()))
            addContent(Element("url").addContent(coverUrl))
            addContent(Element("width").addContent(cover.width.toString()))
        }

        addContent(image)
        addContent(itunesImage)
    }
}
