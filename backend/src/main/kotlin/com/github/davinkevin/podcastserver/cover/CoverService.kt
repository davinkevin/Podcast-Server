package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.storage.CoverExistsRequest
import com.github.davinkevin.podcastserver.service.storage.DeleteRequest
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
            .forEach { file.delete(it) }
    }

}

private fun DeleteRequest.ForCover.toCoverExistsRequest() = CoverExistsRequest.ForItem(
    id = id,
    podcastTitle = podcast.title,
    coverExtension = extension
)
