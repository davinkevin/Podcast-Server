package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.io.FilenameUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.time.format.DateTimeFormatter
import java.util.*

class PlaylistHandler(
        private val playlistService: PlaylistService
) {

    suspend fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val playlists = playlistService
            .findAll()
            .map { PlaylistHAL(it.id, it.name) }
            .toList()

        return ok().bodyValueAndAwait(FindAllPlaylistHAL(playlists))
    }

    suspend fun save(r: ServerRequest): ServerResponse {
        val body = r.awaitBody<SavePlaylist>()

        val savedPlaylist = playlistService.save(body.name)

        return ok().bodyValueAndAwait(savedPlaylist.toHAL())
    }

    suspend fun findById(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))

        val playlist = playlistService.findById(id)
            ?: return notFound().buildAndAwait()

        return ok().bodyValueAndAwait(playlist.toHAL())
    }

    suspend fun deleteById(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))

        playlistService.deleteById(id)

        return noContent().buildAndAwait()
    }

    suspend fun rss(r: ServerRequest): ServerResponse {
        val host = r.extractHost()
        val id = UUID.fromString(r.pathVariable("id"))

        val playlist = playlistService.findById(id)
            ?: return notFound().buildAndAwait()

        val rss = playlist.toRss(host)
            .let { XMLOutputter(Format.getPrettyFormat()).outputString(Document(it)) }

        return ok().contentType(MediaType.APPLICATION_XML).bodyValueAndAwait(rss)
    }

    suspend fun addToPlaylist(r: ServerRequest): ServerResponse {
        val playlistId = UUID.fromString(r.pathVariable("id"))
        val itemId = UUID.fromString(r.pathVariable("itemId"))

        val playlist = playlistService.addToPlaylist(playlistId, itemId)

        return ok().bodyValueAndAwait(playlist.toHAL())
    }

    suspend fun removeFromPlaylist(r: ServerRequest): ServerResponse {
        val playlistId = UUID.fromString(r.pathVariable("id"))
        val itemId = UUID.fromString(r.pathVariable("itemId"))

        val playlist = playlistService.removeFromPlaylist(playlistId, itemId)

        return ok().bodyValueAndAwait(playlist.toHAL())
    }

}

private class SavePlaylist(val name: String)

private class FindAllPlaylistHAL(val content: Collection<PlaylistHAL>)
private class PlaylistHAL(val id: UUID, val name: String)

private fun PlaylistWithItems.toHAL() = PlaylistWithItemsHAL(
    id = this.id,
    name = this.name,
    items = this.items.map(PlaylistWithItems.Item::toHAL)
)

private class PlaylistWithItemsHAL(val id: UUID, val name: String, val items: Collection<Item>) {
    data class Item(
            val id: UUID,
            val title: String,

            val proxyURL: URI,
            val description: String?,
            val mimeType: String,

            val podcast: Podcast,
            val cover: Cover) {

        data class Podcast(val id: UUID, val title: String)
        data class Cover (val id: UUID, val width: Int, val height: Int, val url: URI)
    }
}

private fun PlaylistWithItems.Item.toHAL(): PlaylistWithItemsHAL.Item {
    val extension = cover.url.extension()

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), "cover.$extension")
            .build(true)
            .toUri()

    val fileName = title
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_") +
            (fileName ?: "").let { "." + FilenameUtils.getExtension(it).substringBeforeLast("?") }

    val itemUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), fileName)
            .build(true)
            .toUri()

    return PlaylistWithItemsHAL.Item(
            id = id,
            title = title,
            proxyURL = itemUrl,

            description = description,
            mimeType = mimeType,

            podcast = PlaylistWithItemsHAL.Item.Podcast(
                    id = podcast.id,
                    title = podcast.title
            ),
            cover = PlaylistWithItemsHAL.Item.Cover(
                    id = cover.id,
                    height = cover.height,
                    width = cover.width,
                    url = coverUrl
            )
    )
}

private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")
private val mediaNS = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")

private fun PlaylistWithItems.toRss(host: URI): Element {

    val url = UriComponentsBuilder
            .fromUri(host)
            .pathSegment("api", "v1", "playlists", this.id.toString(), "rss")
            .build(true)
            .toUriString()

    val items = this.items.map { toRssItem(it, host) }

    val channel = Element("channel").apply {
        addContent(Element("title").addContent(Text(this@toRss.name)))
        addContent(Element("link").addContent(Text(url)))
        addContent(items)
    }

    return Element("rss").apply {
        addContent(channel)
        addNamespaceDeclaration(itunesNS)
    }
}

private fun toRssItem(item: PlaylistWithItems.Item, host: URI): Element {
    val coverExtension = item.cover.url.extension()

    val coverUrl = UriComponentsBuilder.fromUri(host)
            .pathSegment("api", "v1", "podcasts", item.podcast.id.toString(), "items", item.id.toString(), "cover.$coverExtension")
            .build(true)
            .toUriString()

    val itunesItemThumbnail = Element("image", itunesNS).setContent(Text(coverUrl))
    val thumbnail = Element("thumbnail", mediaNS).setAttribute("url", coverUrl)

    val extension = Optional.ofNullable(item.fileName)
            .map { FilenameUtils.getExtension(it) }
            .map { it.substringBeforeLast("?") }
            .map { ".$it" }
            .orElse("")

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
