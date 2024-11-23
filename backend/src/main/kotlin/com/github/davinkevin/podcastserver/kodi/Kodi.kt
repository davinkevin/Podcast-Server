package com.github.davinkevin.podcastserver.kodi

import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*

data class Podcast(
    val id: UUID,
    val title: String,
)

data class Item(
    val id: UUID,
    val title: String,
    val pubDate: OffsetDateTime,

    val length: Long,
    val fileName: Path?,
    val mimeType: String,
)