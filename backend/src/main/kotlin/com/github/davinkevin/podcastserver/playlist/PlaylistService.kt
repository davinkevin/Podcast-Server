package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.storage.DownloadAndUploadRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import java.net.URI
import java.util.*

class PlaylistService(
    private val repository: PlaylistRepository,
    private val imageService: ImageService,
    private val fileService: FileStorageService,
) {

    fun findAll(): List<Playlist> = repository.findAll()
    fun findById(id: UUID): PlaylistWithItems? = repository.findById(id)

    fun save(request: SaveRequest): PlaylistWithItems {
        val cover = imageService.fetchCoverInformation(request.coverUrl)
            ?: error("cover ${request.coverUrl} is not available")

        val saveRequest = PlaylistRepository.SaveRequest(
            name = request.name,
            cover = PlaylistRepository.SaveRequest.Cover(
                url = cover.url,
                width = cover.width,
                height = cover.height
            )
        )

        val saveResponse = repository.save(saveRequest)

        saveResponse.toUploadRequest()
            .let(fileService::downloadAndUpload)

        return saveResponse
    }
    data class SaveRequest(val name: String, val coverUrl: URI)

    fun deleteById(id: UUID) = repository.deleteById(id)
    fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems =
        repository.addToPlaylist(playlistId, itemId)
    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems =
        repository.removeFromPlaylist(playlistId, itemId)
}

internal fun PlaylistWithItems.toUploadRequest() = DownloadAndUploadRequest.ForPlaylistCover(
    name = name,
    cover = DownloadAndUploadRequest.ForPlaylistCover.Cover(
        id = cover.id,
        url = cover.url,
    )
)