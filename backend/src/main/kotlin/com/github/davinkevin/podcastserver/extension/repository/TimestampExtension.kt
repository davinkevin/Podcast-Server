package com.github.davinkevin.podcastserver.extension.repository

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Created by kevin on 2019-02-12
 */
private val oppositeOffset = ZoneOffset.ofTotalSeconds(-OffsetDateTime.now().offset.totalSeconds)

fun LocalDateTime?.toUTC(): OffsetDateTime? = this?.toInstant(ZoneOffset.UTC)?.atOffset(ZoneOffset.UTC)?.withOffsetSameLocal(oppositeOffset)
