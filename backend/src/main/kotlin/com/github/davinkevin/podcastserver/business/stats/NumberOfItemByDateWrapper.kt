package com.github.davinkevin.podcastserver.business.stats

import java.time.LocalDate

/**
 * Created by kevin on 06/04/15.
 */
data class NumberOfItemByDateWrapper(val date: LocalDate, val numberOfItems: Int){
    override fun equals(other: Any?) = when (other) {
        null -> false
        !is NumberOfItemByDateWrapper -> false
        else -> date == other.date
    }

    override fun hashCode(): Int {
        return date.hashCode()
    }
}