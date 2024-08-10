package com.github.davinkevin.podcastserver.service.storage

import java.nio.file.Path
import java.util.*

interface CoverExists {
    fun coverExists(r: CoverExistsRequest): Path?
}

sealed class CoverExistsRequest {
    data class ForPlaylist  (val name: String,         val id: UUID, val coverExtension: String): CoverExistsRequest()
    data class ForPodcast   (val title: String,        val id: UUID, val coverExtension: String): CoverExistsRequest()
    data class ForItem      (val podcastTitle: String, val id: UUID, val coverExtension: String): CoverExistsRequest()
}