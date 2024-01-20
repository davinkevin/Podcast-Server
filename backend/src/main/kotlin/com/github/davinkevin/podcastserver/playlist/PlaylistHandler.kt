package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.rss.PlaylistChannel
import com.github.davinkevin.podcastserver.rss.itunesNS
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.rss.Item as RssItem

class PlaylistHandler(
        private val playlistService: PlaylistService
) {

    fun save(r: ServerRequest): Mono<ServerResponse> {
        return r
                .bodyToMono<SavePlaylist>()
                .flatMap { playlistService.save(it.name) }
                .map { PlaylistWithItemsHAL(
                        id = it.id,
                        name = it.name,
                        items = it.items.map(PlaylistWithItems.Item::toHAL)
                ) }
                .flatMap { ok().bodyValue(it) }

    }

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            playlistService
                    .findAll()
                    .map { PlaylistHAL(it.id, it.name) }
                    .collectList()
                    .map { FindAllPlaylistHAL(it) }
                    .flatMap { ok().bodyValue(it) }

    fun findById(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))

        return playlistService
                .findById(id)
                .map { PlaylistWithItemsHAL(
                        id = it.id,
                        name = it.name,
                        items = it.items.map(PlaylistWithItems.Item::toHAL)
                ) }
                .flatMap { ok().bodyValue(it) }
    }

    fun deleteById(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))

        return playlistService
                .deleteById(id)
                .then(noContent().build())
    }

    fun rss(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        val id = UUID.fromString(r.pathVariable("id"))

        return playlistService
                .findById(id)
                .map { it.toRss(host) }
                .map { XMLOutputter(Format.getPrettyFormat()).outputString(Document(it)) }
                .flatMap { ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .bodyValue(it)
                }

    }

    fun addToPlaylist(r: ServerRequest): Mono<ServerResponse> {
        val playlistId = UUID.fromString(r.pathVariable("id"))
        val itemId = UUID.fromString(r.pathVariable("itemId"))

        return playlistService
                .addToPlaylist(playlistId, itemId)
                .map { PlaylistWithItemsHAL(
                        id = it.id,
                        name = it.name,
                        items = it.items.map(PlaylistWithItems.Item::toHAL)
                ) }
                .flatMap { ok().bodyValue(it) }
    }

    fun removeFromPlaylist(r: ServerRequest): Mono<ServerResponse> {
        val playlistId = UUID.fromString(r.pathVariable("id"))
        val itemId = UUID.fromString(r.pathVariable("itemId"))

        return playlistService
                .removeFromPlaylist(playlistId, itemId)
                .map { PlaylistWithItemsHAL(
                        id = it.id,
                        name = it.name,
                        items = it.items.map(PlaylistWithItems.Item::toHAL)
                ) }
                .flatMap { ok().bodyValue(it) }
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

private fun PlaylistWithItems.toRss(host: URI): Element {
    val channel = PlaylistChannel(
        playlist = PlaylistChannel.Playlist(
            id = id,
            name = name
        ),
        host = host,
    )
        .toElement()

    this.items
        .map { it.toRssItem(host) }
        .apply(channel::addContent)

    return Element("rss").apply {
        addContent(channel)
        addNamespaceDeclaration(itunesNS)
    }
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
