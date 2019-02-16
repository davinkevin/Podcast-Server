package com.github.davinkevin.podcastserver.extension.repository

import java.sql.Timestamp
import java.time.ZoneOffset

/**
 * Created by kevin on 2019-02-12
 */
fun Timestamp?.toUTC() = this?.toInstant()?.atOffset(ZoneOffset.UTC)