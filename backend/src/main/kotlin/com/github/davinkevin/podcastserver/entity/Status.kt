package com.github.davinkevin.podcastserver.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.github.davinkevin.podcastserver.database.enums.ItemStatus

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

fun Status.toDb(): ItemStatus = when(this) {
    Status.NOT_DOWNLOADED -> ItemStatus.NOT_DOWNLOADED
    Status.STARTED -> ItemStatus.STARTED
    Status.PAUSED -> ItemStatus.PAUSED
    Status.DELETED -> ItemStatus.DELETED
    Status.STOPPED -> ItemStatus.STOPPED
    Status.FAILED -> ItemStatus.FAILED
    Status.FINISH -> ItemStatus.FINISH
}

fun ItemStatus.fromDb(): Status = when(this) {
    ItemStatus.NOT_DOWNLOADED -> Status.NOT_DOWNLOADED
    ItemStatus.STARTED -> Status.STARTED
    ItemStatus.PAUSED -> Status.PAUSED
    ItemStatus.DELETED -> Status.DELETED
    ItemStatus.STOPPED -> Status.STOPPED
    ItemStatus.FAILED -> Status.FAILED
    ItemStatus.FINISH -> Status.FINISH
}
