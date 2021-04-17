package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.FileService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.time.OffsetDateTime

class CoverService(
    private val cover: CoverRepository,
    private val file: FileService
) {

    suspend fun deleteCoversInFileSystemOlderThan(date: OffsetDateTime) {
        cover
            .findCoverOlderThan(date)
            .filter { file.coverExists(it.podcast.title, it.item.id, it.extension).hasElement().awaitFirst() }
            .collect { file.deleteCover(it).awaitFirstOrNull() }
    }

}
