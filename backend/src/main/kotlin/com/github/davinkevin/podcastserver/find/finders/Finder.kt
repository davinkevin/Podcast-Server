package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Finder::class.java)
/**
 * Created by kevin on 22/02/15.
 */
interface Finder {
    fun findPodcastInformation(url: String): FindPodcastInformation?
    fun compatibility(url: String): Int
}
