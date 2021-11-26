package com.github.davinkevin.podcastserver.cover

import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

@WebFluxTest(controllers = [CoverHandler::class])
@Import(CoverRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class CoverHandlerTest(
    @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var cover: CoverService

    @Nested
    @DisplayName("should delete cover")
    inner class ShouldDeleteCover {

        @Test
        fun `with number of days provided as query param`() {
            /* Given */
            val expectedDate = fixedDate.minusDays(2)
            whenever(cover.deleteCoversInFileSystemOlderThan(expectedDate)).thenReturn(Mono.empty())

            /* When */
            rest
                    .delete()
                    .uri("/api/v1/covers?days=2")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(cover, times(1)).deleteCoversInFileSystemOlderThan(expectedDate)
        }

        @Test
        fun `with default number of days`() {
            /* Given */
            val expectedDate = fixedDate.minusDays(365)
            whenever(cover.deleteCoversInFileSystemOlderThan(expectedDate)).thenReturn(Mono.empty())

            /* When */
            rest
                    .delete()
                    .uri("/api/v1/covers")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(cover, times(1)).deleteCoversInFileSystemOlderThan(fixedDate.minusDays(365))
        }


    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun fixedClock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
    }

}
