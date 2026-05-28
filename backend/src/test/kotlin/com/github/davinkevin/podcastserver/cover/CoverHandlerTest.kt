package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.RestTestClient
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

@NestedSpringTest
@WebMvcTest(controllers = [CoverHandler::class])
@Import(CoverRoutingConfig::class)
@ImportAutoConfiguration(ErrorMvcAutoConfiguration::class)
@AutoConfigureRestTestClient
class CoverHandlerTest(
    @Autowired val rest: RestTestClient
) {

    @MockitoBean private lateinit var cover: CoverService
    @MockitoBean private lateinit var parameters: PodcastServerParameters

    @BeforeEach
    fun setUp() {
        whenever(parameters.numberOfDayToSaveCover).thenReturn(365L)
    }

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
