package com.github.davinkevin.podcastserver.service.storage

import java.net.URI
import java.nio.file.Path

interface ToExternalUrl {
    fun toExternalUrl(r: ExternalUrlRequest): URI
}

sealed class ExternalUrlRequest(open val host: URI) {
    data class ForPlaylist(override val host: URI, val playlistName: String, val file: Path): ExternalUrlRequest(host)
    data class ForPodcast(override val host: URI, val podcastTitle: String, val file: Path): ExternalUrlRequest(host)
    data class ForItem(override val host: URI, val podcastTitle: String, val file: Path): ExternalUrlRequest(host)
}