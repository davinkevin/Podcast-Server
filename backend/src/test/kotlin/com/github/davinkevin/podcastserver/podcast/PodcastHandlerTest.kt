package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper
import com.github.davinkevin.podcastserver.controller.assertThatJson
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.any
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
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.nio.file.Path
import java.time.LocalDate
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

    @Nested
    @DisplayName("should find stats")
    inner class ShouldFindStats {

        @Nested
        @DisplayName("by pubDate")
        inner class ByPubDate {

            @Test
            fun `with some data`() {
                /* Given */
                val r = listOf(
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
                )
                whenever(podcastService.findStatByPubDate(podcast.id, 3)).thenReturn(r.toFlux())
                /* When */
                rest.get()
                        .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byPubDate")
                                .queryParam("numberOfMonths", 3)
                                .build()
                        }
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .assertThatJson { isEqualTo(""" [
                               { "date":"2019-01-02", "numberOfItems":3 },
                               { "date":"2019-01-12", "numberOfItems":2 },
                               { "date":"2019-01-28", "numberOfItems":6 }
                        ] """) }
            }

            @Test
            fun `with no data`() {
                /* Given */
                whenever(podcastService.findStatByPubDate(podcast.id, 3)).thenReturn(Flux.empty())
                /* When */
                rest.get()
                        .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byPubDate")
                                .queryParam("numberOfMonths", 3)
                                .build()
                        }
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .assertThatJson { isArray.isEmpty() }
            }
        }

        @Nested
        @DisplayName("by downloadDate")
        inner class ByDownloadDate {

            @Test
            fun `with some data`() {
                /* Given */
                val r = listOf(
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
                )
                whenever(podcastService.findStatByDownloadDate(podcast.id, 3)).thenReturn(r.toFlux())
                /* When */
                rest.get()
                        .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byDownloadDate")
                                .queryParam("numberOfMonths", 3)
                                .build()
                        }
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .assertThatJson { isEqualTo(""" [
                               { "date":"2019-01-02", "numberOfItems":3 },
                               { "date":"2019-01-12", "numberOfItems":2 },
                               { "date":"2019-01-28", "numberOfItems":6 }
                        ] """) }
            }

            @Test
            fun `with no data`() {
                /* Given */
                whenever(podcastService.findStatByDownloadDate(podcast.id, 3)).thenReturn(Flux.empty())
                /* When */
                rest.get()
                        .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byDownloadDate")
                                .queryParam("numberOfMonths", 3)
                                .build()
                        }
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .assertThatJson { isArray.isEmpty() }
            }
        }

        @Nested
        @DisplayName("by creationDate")
        inner class ByCreationDate {

            @Test
            fun `with some data`() {
                /* Given */
                val r = listOf(
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                        NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
                )
                whenever(podcastService.findStatByCreationDate(podcast.id, 3)).thenReturn(r.toFlux())
                /* When */
                rest.get()
                        .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byCreationDate")
                                .queryParam("numberOfMonths", 3)
                                .build()
                        }
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .assertThatJson { isEqualTo(""" [
                               { "date":"2019-01-02", "numberOfItems":3 },
                               { "date":"2019-01-12", "numberOfItems":2 },
                               { "date":"2019-01-28", "numberOfItems":6 }
                        ] """) }
            }

            @Test
            fun `with no data`() {
                /* Given */
                whenever(podcastService.findStatByCreationDate(podcast.id, 3)).thenReturn(Flux.empty())
                /* When */
                rest.get()
                        .uri { it.path("/api/v1/podcasts/${podcast.id}/stats/byCreationDate")
                                .queryParam("numberOfMonths", 3)
                                .build()
                        }
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .assertThatJson { isArray.isEmpty() }
            }
        }


    }
}
