package com.github.davinkevin.podcastserver.cover

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.queryParamOrNull
import java.time.Clock
import java.time.OffsetDateTime.now

class CoverHandler(
    private val cover: CoverService,
    private val clock: Clock
) {

    suspend fun deleteOldCovers(r: ServerRequest): ServerResponse {
        val retentionNumberOfDays = r.queryParamOrNull("days") ?: "365"

        val date = now(clock).minusDays(retentionNumberOfDays.toLong())

        cover.deleteCoversInFileSystemOlderThan(date)

        return ok().buildAndAwait()
    }

}
