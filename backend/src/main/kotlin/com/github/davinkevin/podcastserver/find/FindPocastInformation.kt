package com.github.davinkevin.podcastserver.find

import java.net.URI

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
