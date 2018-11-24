package com.github.davinkevin.podcastserver.entity

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonCreator
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.control.Option

enum class Status {
    NOT_DOWNLOADED,
    STARTED,
    PAUSED,
    DELETED,
    STOPPED,
    FAILED,
    FINISH;

    companion object {

        private val values = setOf(*Status.values())

        @JsonCreator
        @JvmStatic
        fun of(v: String): Status {
            return fromString(v)
                    .getOrElse { throw IllegalArgumentException("No enum constant Status.$v") }
        }

        @JvmStatic
        fun from(v: String): Option<Status> {
            return fromString(v).toVΛVΓ()
        }

        private fun fromString(v: String) = values.firstOption { s -> s.toString() == v }
    }

}
