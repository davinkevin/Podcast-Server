package com.github.davinkevin.podcastserver.playlist

import java.util.*

class PlaylistService(
        private val repository: PlaylistRepository
) {

    fun findAll(): List<Playlist> = repository.findAll()
    fun findById(id: UUID): PlaylistWithItems? = repository.findById(id)
    fun save(name: String): PlaylistWithItems = repository.save(name)
    fun deleteById(id: UUID) = repository.deleteById(id)
    fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems =
        repository.addToPlaylist(playlistId, itemId)
    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems =
        repository.removeFromPlaylist(playlistId, itemId)

}
