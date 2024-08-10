package com.github.davinkevin.podcastserver.service.storage

import java.nio.file.Path
import java.util.*

interface CoverExists {
    fun coverExists(r: CoverExistsRequest): Path?
}

sealed class CoverExistsRequest {
    data class ForPlaylist(val id: UUID, val name: String, val coverExtension: String): CoverExistsRequest()
    data class ForPodcast(val id: UUID, val title: String, val coverExtension: String): CoverExistsRequest()
    data class ForItem(val id: UUID, val podcastTitle: String, val coverExtension: String): CoverExistsRequest()
}