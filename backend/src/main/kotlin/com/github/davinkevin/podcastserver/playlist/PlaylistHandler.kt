package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.java.net.extension
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*
import kotlin.io.path.extension

class PlaylistHandler(
    private val playlistService: PlaylistService
) {

    private val defaultCoverUrl = URI.create("https://placeholder.io/600x600")

    fun create(r: ServerRequest): Mono<ServerResponse> {
        return r
            .bodyToMono<PlaylistToSaveHAL>()
            .map { PlaylistForCreationV2(it.name, it.cover?.url ?: defaultCoverUrl) }
            .flatMap(playlistService::create)
            .map { PlaylistHAL(id = it.id, name = it.name) }
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
            .map { PlaylistHAL(
                id = it.id,
                name = it.name,
//                items = it.items.map(PlaylistWithItems.Item::toHAL),
            )
            }
            .flatMap { ok().bodyValue(it) }
    }

    fun deleteById(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("id"))

        return playlistService
            .deleteById(id)
            .then(noContent().build())
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
            )
            }
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
            )
            }
            .flatMap { ok().bodyValue(it) }
    }

}

private class PlaylistToSaveHAL(val name: String, val cover: Cover?) {
    data class Cover(val url: URI)
}

private class FindAllPlaylistHAL(val content: Collection<PlaylistHAL>)
private class PlaylistHAL(val id: UUID, val name: String) {
    data class Cover (val id: UUID, val width: Int, val height: Int, val url: URI)
}

private class PlaylistWithItemsHAL(val id: UUID, val name: String, val items: Collection<Item>) {
    data class Cover (val id: UUID, val width: Int, val height: Int, val url: URI)

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

    val extension = this.fileName?.extension?.let { ".$it" } ?: ""
    val fileName = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + extension

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
