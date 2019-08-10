package com.github.davinkevin.podcastserver.extension.repository

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Created by kevin on 2019-02-12
 */
private val oppositeOffset = ZoneOffset.ofTotalSeconds(-OffsetDateTime.now().offset.totalSeconds)

fun Timestamp?.toUTC(): OffsetDateTime? = this?.toInstant()?.atOffset(ZoneOffset.UTC)?.withOffsetSameLocal(oppositeOffset)
fun OffsetDateTime?.toTimestamp() = if(this == null) null else Timestamp.valueOf(atZoneSameInstant(ZoneOffset.UTC)?.toLocalDateTime())
