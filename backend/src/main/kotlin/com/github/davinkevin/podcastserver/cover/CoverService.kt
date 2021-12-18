package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.FileStorageService
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

class CoverService(
        private val cover: CoverRepository,
        private val file: FileStorageService
) {
    
    fun deleteCoversInFileSystemOlderThan(date: OffsetDateTime): Mono<Void> {
        return cover
                .findCoverOlderThan(date)
                .filterWhen { file.coverExists(it.podcast.title, it.item.id, it.extension).hasElement() }
                .flatMap { file.deleteCover(it) }
                .then()
    }

}
