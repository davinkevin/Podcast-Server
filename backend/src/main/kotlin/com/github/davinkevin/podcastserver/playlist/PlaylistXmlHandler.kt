package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.extension

private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")
private val mediaNS = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")

class PlaylistXmlHandler(
    private val playlistService: PlaylistService,
) {

    fun rss(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        val id = UUID.fromString(r.pathVariable("id"))

        TODO()
//        return playlistService
//            .findById(id)
//            .map { it.toRss(host) }
//            .map { XMLOutputter(Format.getPrettyFormat()).outputString(Document(it)) }
//            .flatMap { ServerResponse.ok()
//                .contentType(MediaType.APPLICATION_XML)
//                .bodyValue(it)
//            }

    }

}

private fun PlaylistWithItems.toRss(host: URI): Element {

    val url = UriComponentsBuilder
        .fromUri(host)
        .pathSegment("api", "v1", "playlists", this.id.toString(), "rss")
        .build(true)
        .toUriString()

//    val coverUrl = cover.url.toASCIIString()
    val coverUrl = "foo"

    val items = this.items.map { toRssItem(it, host) }

    val channel = Element("channel").apply {
        addContent(Element("title").addContent(Text(this@toRss.name)))
        addContent(Element("link").addContent(Text(url)))
        addContent(items)

        val itunesImage = Element(
            "image",
            Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")
        ).apply {
            addContent(Text(coverUrl))
        }

//        val image = Element("image").apply {
//            addContent(Element("height").addContent(cover.height.toString()))
//            addContent(Element("url").addContent(coverUrl))
//            addContent(Element("width").addContent(cover.width.toString()))
//        }
//
//        addContent(image)
        addContent(itunesImage)
    }

    return Element("rss").apply {
        addContent(channel)
        addNamespaceDeclaration(itunesNS)
    }
}

private fun toRssItem(item: PlaylistWithItems.Item, host: URI): Element {
    val coverExtension = item.cover.url.extension().ifBlank { "jpg" }

    val coverUrl = UriComponentsBuilder.fromUri(host)
        .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString(), "cover.$coverExtension")
        .build(true)
        .toUriString()

    val itunesItemThumbnail = Element("image", itunesNS).setContent(Text(coverUrl))
    val thumbnail = Element("thumbnail", mediaNS).setAttribute("url", coverUrl)

    val extension = item.fileName?.extension?.let { ".$it" } ?: ""
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

        if(item.mimeType.isNotEmpty()) {
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
