package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.storage.CoverExistsRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import java.time.OffsetDateTime

class CoverService(
    private val cover: CoverRepository,
    private val file: FileStorageService
) {
    fun deleteCoversInFileSystemOlderThan(date: OffsetDateTime) {
        cover
            .findCoverOlderThan(date)
            .asSequence()
            .filter { file.coverExists(it.toCoverExistsRequest()) != null }
            .forEach { file.deleteCover(it) }
    }

}

private fun DeleteCoverRequest.toCoverExistsRequest() = CoverExistsRequest.ForItem(
    id = id,
    podcastTitle = podcast.title,
    coverExtension = extension
)
