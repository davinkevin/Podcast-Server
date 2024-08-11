package com.github.davinkevin.podcastserver.service.storage

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

interface DeleteObject {
    fun delete(request: DeleteRequest): Boolean
}

sealed class DeleteRequest {
    data class ForPodcast(val id: UUID, val title: String): DeleteRequest()
    data class ForItem(val id: UUID, val fileName: Path, val podcastTitle: String): DeleteRequest() {
        val path: Path = Paths.get(podcastTitle).resolve(fileName)
    }
    data class ForCover(val id: UUID, val extension: String, val item: Item, val podcast: Podcast): DeleteRequest() {
        data class Podcast(val id: UUID, val title: String)
        data class Item(val id: UUID, val title: String)
    }
}