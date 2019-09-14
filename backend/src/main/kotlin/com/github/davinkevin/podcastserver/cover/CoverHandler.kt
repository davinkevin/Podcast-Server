package com.github.davinkevin.podcastserver.cover

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetDateTime

class CoverHandler(
        private val cover: CoverService,
        private val clock: Clock
) {

    fun deleteOldCovers(r: ServerRequest): Mono<ServerResponse> {
        val retentionNumberOfDays = r.queryParam("days")
                .map { it.toLong() }
                .orElse(365L)

        val date = OffsetDateTime.now(clock)
                .minusDays(retentionNumberOfDays)

        return cover
                .deleteCoversInFileSystemOlderThan(date)
                .then(ok().build())
    }

}
