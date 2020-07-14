package com.github.davinkevin.podcastserver.update

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.*

/**
 * Created by kevin on 14/07/2020
 */
@WebFluxTest(controllers = [UpdateHandler::class])
@Import(UpdateRouterConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class UpdateHandlerTest(
        @Autowired val rest: WebTestClient
) {
    @MockBean private lateinit var update: UpdateService

    @Nested
    @DisplayName("should update all")
    inner class ShouldUpdateAll {

        @Test
        fun `with default values`() {
            /* Given */
            whenever(update.updateAll(force = false, download = false))
                    .thenReturn(Mono.empty())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/update")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(update)
                    .updateAll(force = false, download = false)
        }

        @Test
        fun forced() {
            /* Given */
            whenever(update.updateAll(force = true, download = false))
                    .thenReturn(Mono.empty())

            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/update?force=true")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(update)
                    .updateAll(force = true, download = false)
        }

        @Test
        fun `and download`() {
            /* Given */
            whenever(update.updateAll(force = false, download = true))
                    .thenReturn(Mono.empty())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/update?download=true")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(update)
                    .updateAll(force = false, download = true)
        }
    }

    @Nested
    @DisplayName("should update podcast")
    inner class ShouldUpdatePodcast {

        @Test
        fun `with success`() {
            /* Given */
            val id = UUID.fromString("cd651e1f-1dbd-4f20-af61-951ec0473884")
            whenever(update.update(id)).thenReturn(Mono.empty())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/$id/update")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk

            verify(update).update(id)
        }

    }

}
