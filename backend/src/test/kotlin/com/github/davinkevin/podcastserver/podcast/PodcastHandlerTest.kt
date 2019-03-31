package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.tag.Tag
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
import java.net.URI
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
            url = "https://foo.bar.com/app/file.rss",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
            type = "RSS",
            tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

            cover = CoverForPodcast(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = URI("https://external.domain.tld/cover.png"),
                    height = 200, width = 200
            )
    )

    @Test
    fun `should find by id`() {
        /* Given */
        whenever(podcastService.findById(podcast.id)).thenReturn(podcast.toMono())
        /* When */
        rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/${podcast.id}")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .assertThatJson {
                    isEqualTo("""{
                       "cover":{
                          "height":200,
                          "id":"1e275238-4cbe-4abb-bbca-95a0e4ebbeea",
                          "url":"/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/cover.png",
                          "width":200
                       },
                       "hasToBeDeleted":true,
                       "id":"dd16b2eb-657e-4064-b470-5b99397ce729",
                       "lastUpdate":"2019-03-31T11:21:32.000000045+01:00",
                       "tags":[
                          {
                             "id":"f9d92927-1c4c-47a5-965d-efbb2d422f0c",
                             "name":"Cinéma"
                          }
                       ],
                       "title":"Podcast title",
                       "type":"RSS",
                       "url":"https://foo.bar.com/app/file.rss"
                    }""")
                }
    }

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
        @DisplayName("globally")
        inner class Globally {

            @Nested
            @DisplayName("by creation date")
            inner class ByCreationDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val youtube = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6))
                    )
                    val rss = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-02"), 5),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 8),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-28"), 1))
                    )
                    whenever(podcastService.findStatByTypeAndCreationDate(3)).thenReturn(Flux.just(youtube, rss))
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byCreationDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo("""{
                                       "content":[ {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-01-02", "numberOfItems":3 },
                                                { "date":"2019-01-12", "numberOfItems":2 },
                                                { "date":"2019-01-28", "numberOfItems":6 }
                                             ]
                                          }, {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-02-02", "numberOfItems":5 },
                                                { "date":"2019-02-12", "numberOfItems":8 },
                                                { "date":"2019-02-28", "numberOfItems":1 }
                                             ]
                                          } ]
                                    }""") }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByTypeAndCreationDate(3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byCreationDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" {"content":[]} """) }
                }
            }

            @Nested
            @DisplayName("by pubDate")
            inner class ByPubDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val youtube = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6))
                    )
                    val rss = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-02"), 5),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 8),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-28"), 1))
                    )
                    whenever(podcastService.findStatByTypeAndPubDate(3)).thenReturn(Flux.just(youtube, rss))
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byPubDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo("""{
                                       "content":[ {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-01-02", "numberOfItems":3 },
                                                { "date":"2019-01-12", "numberOfItems":2 },
                                                { "date":"2019-01-28", "numberOfItems":6 }
                                             ]
                                          }, {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-02-02", "numberOfItems":5 },
                                                { "date":"2019-02-12", "numberOfItems":8 },
                                                { "date":"2019-02-28", "numberOfItems":1 }
                                             ]
                                          } ]
                                    }""") }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByTypeAndPubDate(3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byPubDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" {"content":[]} """) }
                }
            }
            @Nested
            @DisplayName("by download date")
            inner class ByDownloadDate {

                @Test
                fun `with some data`() {
                    /* Given */
                    val youtube = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6))
                    )
                    val rss = StatsPodcastType("YOUTUBE", setOf(
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-02"), 5),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 8),
                            NumberOfItemByDateWrapper(LocalDate.parse("2019-02-28"), 1))
                    )
                    whenever(podcastService.findStatByTypeAndDownloadDate(3)).thenReturn(Flux.just(youtube, rss))
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byDownloadDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo("""{
                                       "content":[ {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-01-02", "numberOfItems":3 },
                                                { "date":"2019-01-12", "numberOfItems":2 },
                                                { "date":"2019-01-28", "numberOfItems":6 }
                                             ]
                                          }, {
                                             "type":"YOUTUBE", "values":[
                                                { "date":"2019-02-02", "numberOfItems":5 },
                                                { "date":"2019-02-12", "numberOfItems":8 },
                                                { "date":"2019-02-28", "numberOfItems":1 }
                                             ]
                                          } ]
                                    }""") }
                }

                @Test
                fun `with no data`() {
                    /* Given */
                    whenever(podcastService.findStatByTypeAndDownloadDate(3)).thenReturn(Flux.empty())
                    /* When */
                    rest.get()
                            .uri { it.path("/api/v1/podcasts/stats/byDownloadDate")
                                    .queryParam("numberOfMonths", 3)
                                    .build()
                            }
                            .exchange()
                            /* Then */
                            .expectStatus().isOk
                            .expectBody()
                            .assertThatJson { isEqualTo(""" {"content":[]} """) }
                }
            }


        }

        @Nested
        @DisplayName("for a given podcast")
        inner class ForAGivenPodcast {

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
                    whenever(podcastService.findStatByPodcastIdAndPubDate(podcast.id, 3)).thenReturn(r.toFlux())
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
                    whenever(podcastService.findStatByPodcastIdAndPubDate(podcast.id, 3)).thenReturn(Flux.empty())
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
                    whenever(podcastService.findStatByPodcastIdAndDownloadDate(podcast.id, 3)).thenReturn(r.toFlux())
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
                    whenever(podcastService.findStatByPodcastIdAndDownloadDate(podcast.id, 3)).thenReturn(Flux.empty())
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
                    whenever(podcastService.findStatByPodcastIdAndCreationDate(podcast.id, 3)).thenReturn(r.toFlux())
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
                    whenever(podcastService.findStatByPodcastIdAndCreationDate(podcast.id, 3)).thenReturn(Flux.empty())
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
}
