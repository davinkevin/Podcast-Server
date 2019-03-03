package com.github.davinkevin.podcastserver.podcast

import java.time.LocalDate
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



data class NumberOfItemByDateWrapper(val date: LocalDate, val numberOfItems: Int){
    override fun equals(other: Any?) = when (other) {
        null -> false
        !is NumberOfItemByDateWrapper -> false
        else -> date == other.date
    }

    override fun hashCode() = date.hashCode()
}

data class StatsPodcastType(val type: String, val values: Set<NumberOfItemByDateWrapper>)
