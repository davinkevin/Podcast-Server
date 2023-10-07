package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.defaultCoverInformation
import com.github.davinkevin.podcastserver.service.image.toCoverForCreation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

class PlaylistService(
    private val repository: PlaylistRepository,
    private val image: ImageService,
) {

    fun findAll(): Flux<Playlist> = repository.findAll()
    fun findById(id: UUID): Mono<PlaylistWithItems> = repository.findById(id)
    fun create(name: String, coverUri: URI): Mono<PlaylistWithItems> {
        return image.fetchCoverInformation(coverUri)
            .switchIfEmpty { defaultCoverInformation.toMono() }
            .map { it.toCoverForCreation() }
            .flatMap { repository.create(PlaylistForCreate(name, it)) }
    }
    fun deleteById(id: UUID): Mono<Void> = repository.deleteById(id)

    fun addToPlaylist(playlistId: UUID, itemId: UUID) = repository.addToPlaylist(playlistId, itemId)
    fun removeFromPlaylist(playlistId: UUID, itemId: UUID) = repository.removeFromPlaylist(playlistId, itemId)

}
