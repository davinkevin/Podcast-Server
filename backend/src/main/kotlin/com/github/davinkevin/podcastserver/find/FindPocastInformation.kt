package com.github.davinkevin.podcastserver.find

import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

/**
 * Created by kevin on 2019-08-11
 */
data class FindPodcastInformation(
        val title: String,
        val description: String = "",
        val url: URI,
        val cover: FindCoverInformation?,
        val type: String
)

data class FindCoverInformation(val height: Int, val width: Int, val url: URI)
