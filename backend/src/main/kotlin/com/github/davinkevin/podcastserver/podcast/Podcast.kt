package com.github.davinkevin.podcastserver.podcast

import java.util.*

class Podcast (
    val id: UUID,
    val title: String,

    val cover: CoverForPodcast
)

class CoverForPodcast(
        val id: UUID,
        val url: String,
        val height: Int,
        val width: Int
)