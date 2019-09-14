package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.FileService
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import com.github.davinkevin.podcastserver.cover.CoverRepositoryV2 as CoverRepository

class CoverService(
        private val cover: CoverRepository,
        private val file: FileService
) {
    
    fun deleteCoversInFileSystemOlderThan(date: OffsetDateTime): Mono<Void> {
        return cover
                .findCoverOlderThan(date)
                .filterWhen { file.coverExists(it.podcast.title, it.item.id, it.extension).hasElement() }
                .flatMap { file.deleteCover(it) }
                .then()
    }

}
