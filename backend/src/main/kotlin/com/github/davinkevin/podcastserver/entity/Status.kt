package com.github.davinkevin.podcastserver.entity

import com.fasterxml.jackson.annotation.JsonCreator

enum class Status {
    NOT_DOWNLOADED,
    STARTED,
    PAUSED,
    DELETED,
    STOPPED,
    FAILED,
    FINISH;

    companion object {

        private val values = setOf(*values())

        @JsonCreator
        @JvmStatic
        fun of(v: String): Status {
            return values
                    .firstOrNull { s -> s.toString() == v }
                    ?: throw IllegalArgumentException("No enum constant Status.$v")
        }

    }

}
