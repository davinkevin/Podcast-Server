package com.github.davinkevin.podcastserver.cover

import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import java.time.Clock
import java.time.OffsetDateTime

class CoverHandler(
        private val cover: CoverService,
        private val clock: Clock
) {

    fun deleteOldCovers(r: ServerRequest): ServerResponse {
        val retentionNumberOfDays = r.paramOrNull("days")?.toLong() ?: 365L

        val date = OffsetDateTime.now(clock)
                .minusDays(retentionNumberOfDays)

        cover.deleteCoversInFileSystemOlderThan(date).block()

        return ServerResponse.ok().build()
    }

}
