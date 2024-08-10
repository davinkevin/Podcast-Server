package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.extension.serverRequest.normalizedURI
import com.github.davinkevin.podcastserver.rss.Item
import com.github.davinkevin.podcastserver.rss.PlaylistChannel
import com.github.davinkevin.podcastserver.rss.rootRss
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.net.URI
import java.util.*

class PlaylistXmlHandler(
    private val playlistService: PlaylistService
) {

    fun rss(r: ServerRequest): ServerResponse {
        val host = r.extractHost()
        val callUrl = r.normalizedURI()
        val id = r.pathVariable("id")
            .let(UUID::fromString)

        val playlist = playlistService.findById(id)
            ?: return ServerResponse.notFound().build()

        val items = playlist.items.map { it.toRssItem(host) }
        val rss = playlist.toRss(callUrl = callUrl)
            .addContent(items)
            .let(::rootRss)

        val body = XMLOutputter(Format.getPrettyFormat()).outputString(Document(rss))

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(body)
    }
}

private fun PlaylistWithItems.toRss(callUrl: URI): Element = PlaylistChannel(
    playlist = PlaylistChannel.Playlist(
        id = id,
        name = name
    ),
    calledURI = callUrl,
    cover = PlaylistChannel.Cover(
        url = this.cover.url,
        height = this.cover.height,
        width = this.cover.width,
    )
)
    .toElement()

private fun PlaylistWithItems.Item.toRssItem(host: URI): Element = Item(
    host = host,
    podcast = Item.Podcast(podcast.id),
    item = Item.Item(
        id = id,
        title = title,
        mimeType = mimeType,
        fileName = fileName,
        pubDate = pubDate,
        description = description,
        length = length,
    ),
    cover = Item.Cover(
        url = cover.url
    )
)
    .toElement()