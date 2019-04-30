package com.github.davinkevin.podcastserver.cover

import java.net.URI
import java.util.*

class Cover(
        val id: UUID,
        val url: URI,
        val height: Int,
        val width: Int
)

data class DownloadPodcastCoverInformation(val podcastTitle: String, val podcastId: UUID, val coverURI: URI)
