package com.github.davinkevin.podcastserver.cover

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

@WebMvcTest(controllers = [CoverHandler::class])
@Import(CoverRoutingConfig::class)
@ImportAutoConfiguration(ErrorMvcAutoConfiguration::class)
class CoverHandlerTest(
    @Autowired val rest: WebTestClient
) {

    @MockitoBean private lateinit var cover: CoverService

    @Nested
    @DisplayName("should delete cover")
    inner class ShouldDeleteCover {

        @Test
        fun `with number of days provided as query param`() {
            /* Given */
            val expectedDate = fixedDate.minusDays(2)

            /* When */
            rest
                    .delete()
                    .uri("/api/v1/covers?days=2")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(cover).deleteCoversInFileSystemOlderThan(expectedDate)
        }

        @Test
        fun `with default number of days`() {
            /* Given */
            val expectedDate = fixedDate.minusDays(365)

            /* When */
            rest
                    .delete()
                    .uri("/api/v1/covers")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(cover).deleteCoversInFileSystemOlderThan(fixedDate.minusDays(365))
        }


    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun fixedClock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
    }

}
