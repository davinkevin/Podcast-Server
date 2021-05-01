package com.github.davinkevin.podcastserver.playlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

class PlaylistService(
        private val repository: PlaylistRepository
) {

    fun findAll(): Flow<Playlist> = repository.findAll()
    suspend fun findById(id: UUID): PlaylistWithItems? = repository.findById(id)
    suspend fun save(name: String): PlaylistWithItems = repository.save(name)
    suspend fun deleteById(id: UUID) = repository.deleteById(id)

    suspend fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems = repository.addToPlaylist(playlistId, itemId)
    suspend fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems = repository.removeFromPlaylist(playlistId, itemId)

}
