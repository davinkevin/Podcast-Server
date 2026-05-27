package com.github.davinkevin.podcastserver.digest

import java.time.OffsetDateTime

class DigestService(private val repository: DigestRepository) {

    fun findActivePodcastsSince(since: OffsetDateTime, maxItemsPerPodcast: Int): List<DigestPodcast> =
        repository.findActivePodcastsSince(since, maxItemsPerPodcast)
}
