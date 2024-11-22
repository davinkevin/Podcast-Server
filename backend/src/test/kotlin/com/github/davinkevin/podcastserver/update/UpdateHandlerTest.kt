package com.github.davinkevin.podcastserver.update

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

/**
 * Created by kevin on 14/07/2020
 */
@WebMvcTest(controllers = [UpdateHandler::class])
@Import(UpdateRouterConfig::class)
@ImportAutoConfiguration(ErrorMvcAutoConfiguration::class)
class UpdateHandlerTest(
        @Autowired val rest: WebTestClient
) {
    @MockitoBean private lateinit var update: UpdateService

    @Nested
    @DisplayName("should update all")
    inner class ShouldUpdateAll {

        @Test
        fun `with default values`() {
            /* Given */
            doNothing().whenever(update).updateAll(force = false, download = false)
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
            doNothing().whenever(update).updateAll(force = true, download = false)

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
            doNothing().whenever(update).updateAll(force = false, download = true)

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
            doNothing().whenever(update).update(id)
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
