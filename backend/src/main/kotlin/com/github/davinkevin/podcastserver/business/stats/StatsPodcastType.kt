package com.github.davinkevin.podcastserver.business.stats

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vavr.API.Set
import io.vavr.collection.Set

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
data class StatsPodcastType(val type: String, val values: Set<NumberOfItemByDateWrapper> = Set()) {

    val isEmpty: Boolean

        @JsonIgnore get() = values.isEmpty
}
