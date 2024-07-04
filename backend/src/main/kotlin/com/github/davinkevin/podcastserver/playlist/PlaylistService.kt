package com.github.davinkevin.podcastserver.playlist

import java.util.*

class PlaylistService(
        private val repository: PlaylistRepository
) {

    fun findAll(): List<Playlist> = repository.findAll().collectList().block()!!
    fun findById(id: UUID): PlaylistWithItems? = repository.findById(id).block()
    fun save(name: String): PlaylistWithItems = repository.save(name).block()!!
    fun deleteById(id: UUID) {
        repository.deleteById(id).block()
    }
    fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems =
        repository.addToPlaylist(playlistId, itemId).block()!!
    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems =
        repository.removeFromPlaylist(playlistId, itemId).block()!!

}
