package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.storage.CoverExistsRequest
import com.github.davinkevin.podcastserver.service.storage.DeleteRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class CoverService(
    private val cover: CoverRepository,
    private val file: FileStorageService
) {
    private val log = LoggerFactory.getLogger(CoverService::class.java)

    fun deleteCoversInFileSystemOlderThan(date: OffsetDateTime) {
        val candidates = cover.findCoverOlderThan(date)
        val present = candidates.filter { file.coverExists(it.toCoverExistsRequest()) != null }
        val deleted = present.count { file.delete(it) }
        val failed = present.size - deleted

        val message = "Covers older than {}: {} candidates, {} present, {} deleted, {} failed"
        if (failed == 0) log.info(message, date, candidates.size, present.size, deleted, failed)
        else log.warn(message, date, candidates.size, present.size, deleted, failed)
    }

}

private fun DeleteRequest.ForCover.toCoverExistsRequest() = CoverExistsRequest.ForItem(
    id = item.id,
    podcastTitle = podcast.title,
    coverExtension = extension
)
