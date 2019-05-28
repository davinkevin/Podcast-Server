package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.tag.Tag
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class Podcast (
    val id: UUID,
    val title: String,
    val description: String?,
    val url: String?,
    val hasToBeDeleted: Boolean,
    val lastUpdate: OffsetDateTime?,
    val type: String,

    val tags: Collection<Tag>,

    val cover: CoverForPodcast
)

data class CoverForPodcast(
        val id: UUID,
        val url: URI,
        val height: Int,
        val width: Int
)

data class NumberOfItemByDateWrapper(val date: LocalDate, val numberOfItems: Int){
    override fun equals(other: Any?) = when (other) {
        null -> false
        !is NumberOfItemByDateWrapper -> false
        else -> date == other.date
    }

    override fun hashCode() = date.hashCode()
}

data class StatsPodcastType(val type: String, val values: Set<NumberOfItemByDateWrapper>)


data class PodcastForCreation(val title: String, val url: URI, val tags: Collection<TagForCreation>, val type: String, val hasToBeDeleted: Boolean, val cover: CoverForCreation)
data class PodcastForUpdate(val id: UUID, val title: String, val url: URI, val hasToBeDeleted: Boolean, val tags: Collection<TagForCreation>, val cover: CoverForCreation)
data class TagForCreation(val id: UUID?, val name: String)
