package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.extension.serverRequest.normalizedURI
import com.github.davinkevin.podcastserver.rss.PlaylistChannel
import com.github.davinkevin.podcastserver.rss.itunesNS
import com.github.davinkevin.podcastserver.rss.rootRss
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.rss.Item as RssItem

class PlaylistHandler(
        private val playlistService: PlaylistService
) {

    fun save(r: ServerRequest): ServerResponse {
        val playlistSaveRequest = r.body<SavePlaylist>()

        val playlist = playlistService.save(playlistSaveRequest.name)

        val body = PlaylistWithItemsHAL(
            id = playlist.id,
            name = playlist.name,
            items = playlist.items.map(PlaylistWithItems.Item::toHAL)
        )

        return ServerResponse.ok().body(body)
    }


    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val playlists = playlistService.findAll()

        val body = playlists
            .map { PlaylistHAL(it.id, it.name) }
            .let(::FindAllPlaylistHAL)

        return ServerResponse.ok().body(body)
    }

    fun findById(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("id")
            .let(UUID::fromString)

        val playlist = playlistService.findById(id)
            ?: return ServerResponse.notFound().build()

        val body = PlaylistWithItemsHAL(
            id = playlist.id,
            name = playlist.name,
            items = playlist.items.map(PlaylistWithItems.Item::toHAL)
        )

        return ServerResponse.ok().body(body)
    }

    fun deleteById(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("id")
            .let(UUID::fromString)

        playlistService.deleteById(id)

        return ServerResponse.noContent().build()
    }

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

    fun addToPlaylist(r: ServerRequest): ServerResponse {
        val playlistId = r.pathVariable("id")
            .let(UUID::fromString)
        val itemId = r.pathVariable("itemId")
            .let(UUID::fromString)

        val playlistWithItem = playlistService.addToPlaylist(playlistId, itemId)

        val body = PlaylistWithItemsHAL(
            id = playlistWithItem.id,
            name = playlistWithItem.name,
            items = playlistWithItem.items.map(PlaylistWithItems.Item::toHAL)
        )

        return ServerResponse.ok().body(body)
    }

    fun removeFromPlaylist(r: ServerRequest): ServerResponse {
        val playlistId = r.pathVariable("id")
            .let(UUID::fromString)
        val itemId = r.pathVariable("itemId")
            .let(UUID::fromString)

        val playlistWithItem = playlistService.removeFromPlaylist(playlistId, itemId)

        val body = PlaylistWithItemsHAL(
            id = playlistWithItem.id,
            name = playlistWithItem.name,
            items = playlistWithItem.items.map(PlaylistWithItems.Item::toHAL)
        )

        return ServerResponse.ok().body(body)
    }
}

private class SavePlaylist(val name: String)

private class FindAllPlaylistHAL(val content: Collection<PlaylistHAL>)
private class PlaylistHAL(val id: UUID, val name: String)

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
    val coverExtension = cover.url.extension().ifBlank { "jpg" }

    val coverUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), "cover.$coverExtension")
            .build(true)
            .toUri()

    val itemUrl = UriComponentsBuilder.fromPath("/")
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", id.toString(), slug())
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

private fun PlaylistWithItems.toRss(callUrl: URI): Element {
    return PlaylistChannel(
        playlist = PlaylistChannel.Playlist(
            id = id,
            name = name
        ),
        calledURI = callUrl,
        cover = PlaylistChannel.Cover(
            url =  this.cover.url,
            height =  this.cover.height,
            width =  this.cover.width,
        )
    )
        .toElement()
}

private fun PlaylistWithItems.Item.toRssItem(host: URI): Element = RssItem(
    host = host,
    podcast = RssItem.Podcast(podcast.id),
    item = RssItem.Item(
        id = id,
        title = title,
        mimeType = mimeType,
        fileName = fileName,
        pubDate = pubDate,
        description = description,
        length = length,
    ),
    cover = RssItem.Cover(
        url = cover.url
    )
)
    .toElement()
