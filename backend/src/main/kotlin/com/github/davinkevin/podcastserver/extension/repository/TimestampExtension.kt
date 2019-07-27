package com.github.davinkevin.podcastserver.extension.repository

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Created by kevin on 2019-02-12
 */
private val oppositeOffset = ZoneOffset.ofTotalSeconds(-OffsetDateTime.now().offset.totalSeconds)

fun Timestamp?.toUTC(): OffsetDateTime? = this?.toInstant()?.atOffset(ZoneOffset.UTC)?.withOffsetSameLocal(oppositeOffset)
fun OffsetDateTime?.toTimestamp() = Timestamp.valueOf(this?.atZoneSameInstant(ZoneOffset.UTC)?.toLocalDateTime())
