package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.item.ItemRoutingConfig
import com.github.davinkevin.podcastserver.item.ItemService
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.*
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
import reactor.core.publisher.toMono
import java.nio.file.Path
import java.util.*

/**
 * Created by kevin on 2019-02-16
 */
@WebFluxTest(controllers = [PodcastHandler::class])
@Import(PodcastRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class PodcastHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var podcastService: PodcastService
    @MockBean lateinit var p: PodcastServerParameters
    @MockBean lateinit var fileService: FileService

    val podcast = Podcast(
            id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast title",

            cover = CoverForPodcast(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = "https://external.domain.tld/cover.png",
                    height = 200, width = 200
            )
    )

    @Nested
    @DisplayName("should find cover")
    inner class ShouldFindCover {

        @Test
        fun `by redirecting to local file server if cover exists locally`() {
            /* Given */
            whenever(podcastService.findById(podcast.id)).thenReturn(podcast.toMono())
            whenever(fileService.exists(any())).then { it.getArgument<Path>(0).toMono() }
            whenever(p.coverDefaultName).thenReturn("cover")
            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{id}/cover.png", podcast.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://localhost:8080/data/Podcast%20title/cover.png")
        }

        @Test
        fun `by redirecting to external file if cover does not exist locally`() {
            /* Given */
            whenever(podcastService.findById(podcast.id)).thenReturn(podcast.toMono())
            whenever(fileService.exists(any())).then { Mono.empty<Path>() }

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{id}/cover.png", podcast.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://external.domain.tld/cover.png")
        }

    }
}
