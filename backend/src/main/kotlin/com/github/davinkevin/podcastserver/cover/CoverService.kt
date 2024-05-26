package com.github.davinkevin.podcastserver.cover

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
            .filter { file.coverExists(it.podcast.title, it.item.id, it.extension).hasElement().block()!! }
            .forEach { file.deleteCover(it).block() }
    }

}
