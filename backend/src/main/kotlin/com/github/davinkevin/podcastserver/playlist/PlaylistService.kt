package com.github.davinkevin.podcastserver.playlist

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

class PlaylistService(
        private val repository: PlaylistRepository
) {

    fun findAll(): Flux<Playlist> = repository.findAll()
    fun findById(id: UUID): Mono<PlaylistWithItems> = repository.findById(id)
    fun save(name: String): Mono<PlaylistWithItems> = repository.save(name)
    fun deleteById(id: UUID): Mono<Void> = repository.deleteById(id)

    fun addToPlaylist(playlistId: UUID, itemId: UUID) = repository.addToPlaylist(playlistId, itemId)
    fun removeFromPlaylist(playlistId: UUID, itemId: UUID) = repository.removeFromPlaylist(playlistId, itemId)

}
