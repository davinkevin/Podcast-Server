package com.github.davinkevin.podcastserver.cover

import java.net.URI
import java.util.*

data class Cover(
        val id: UUID,
        val url: URI,
        val height: Int,
        val width: Int
)
