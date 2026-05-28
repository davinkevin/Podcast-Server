package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import org.springframework.web.servlet.function.paramOrNull
import java.time.Clock
import java.time.OffsetDateTime

class CoverHandler(
        private val cover: CoverService,
        private val clock: Clock,
        private val parameters: PodcastServerParameters,
) {

    fun deleteOldCovers(r: ServerRequest): ServerResponse {
        val retentionNumberOfDays = r.paramOrNull("days")?.toLong() ?: parameters.numberOfDayToSaveCover

        val date = OffsetDateTime.now(clock)
                .minusDays(retentionNumberOfDays)

        cover.deleteCoversInFileSystemOlderThan(date)

        return ServerResponse.ok().build()
    }

    fun findDaysToSave(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse =
        ServerResponse.ok().body(parameters.numberOfDayToSaveCover)

    fun updateDaysToSave(r: ServerRequest): ServerResponse {
        val value = r.body<Long>()
        parameters.updateNumberOfDayToSaveCover(value)
        return ServerResponse.ok().body(value)
    }
}
