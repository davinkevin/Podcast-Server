package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.java.net.extension
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.service.storage.CoverExistsRequest
import com.github.davinkevin.podcastserver.service.storage.ExternalUrlRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Path
import java.util.*

class PlaylistHandler(
        private val playlistService: PlaylistService,
        private val file: FileStorageService,
) {

    fun save(r: ServerRequest): ServerResponse {
        val request = r.body<SaveRequest>().toSaveRequest()

        val playlist = playlistService.save(request)

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

    fun cover(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("id").let(UUID::fromString)

        val playlist = playlistService.findById(id)
            ?: return ServerResponse.notFound().build()

        val coverExistsRequest = playlist.toCoverExistsRequest()
        val uri = when(val coverPath = file.coverExists(coverExistsRequest)) {
            is Path -> ExternalUrlRequest.ForPlaylist(
                host = r.extractHost(),
                playlistName = playlist.name,
                file = coverPath
            )
                .let(file::toExternalUrl)
            else -> playlist.cover.url
        }

        return ServerResponse.seeOther(uri).build()
    }

    fun deleteById(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("id")
            .let(UUID::fromString)

        playlistService.deleteById(id)

        return ServerResponse.noContent().build()
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

private data class SaveRequest(
    val name: String,
    val coverUrl: URI = URI.create("https://placehold.co/600x600.png?text=no+cover")
)

private fun SaveRequest.toSaveRequest() = PlaylistService.SaveRequest(
    name = name,
    coverUrl = coverUrl
)

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

internal fun PlaylistWithItems.toCoverExistsRequest() = CoverExistsRequest.ForPlaylist(
    name = name,
    id = cover.id,
    coverExtension = cover.url.extension()
)