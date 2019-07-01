package com.github.davinkevin.podcastserver.watchlist

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
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
import reactor.core.publisher.Flux
import java.util.*

/**
 * Created by kevin on 2019-07-06
 */
@WebFluxTest(controllers = [WatchListHandler::class])
@Import(WatchListRoutingConfig::class, WatchListHandler::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class WatchListHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var service: WatchListService

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        @Test
        fun `with no watch list`() {
            /* Given */
            whenever(service.findAll()).thenReturn(Flux.empty())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/watchlists")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{ "content":[] }""")
                    }
        }

        @Test
        fun `with watch lists in results`() {
            /* Given */
            whenever(service.findAll()).thenReturn(Flux.just(
                    WatchList(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"),
                    WatchList(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"),
                    WatchList(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third")
            ))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/watchlists")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                           "content":[
                              { "id":"05621536-b211-4736-a1ed-94d7ad494fe0", "name":"first" },
                              { "id":"6e15b195-7a1f-43e8-bc06-bf88b7f865f8", "name":"second" },
                              { "id":"37d09949-6ae0-4b8b-8cc9-79ffd541e51b", "name":"third" }
                           ]
                        }""")
                    }
        }

    }

}
