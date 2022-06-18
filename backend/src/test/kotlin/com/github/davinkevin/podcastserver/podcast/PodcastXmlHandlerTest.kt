package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.item.*
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.tag.Tag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@WebFluxTest(controllers = [PodcastXmlHandler::class])
@Import(PodcastRoutingConfig::class, PodcastHandler::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class PodcastXmlHandlerTest(
    @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var fileService: FileStorageService
    @MockBean private lateinit var itemService: ItemService
    @MockBean private lateinit var podcastService: PodcastService

    val podcast = Podcast(
        id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
        title = "Podcast title",
        description = "desc",
        signature = null,
        url = "https://foo.bar.com/app/file.rss",
        hasToBeDeleted = true,
        lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
        type = "RSS",
        tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

        cover = Cover(
            id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
            url = URI("https://external.domain.tld/cover.png"),
            height = 200, width = 200
        )
    )

    @Nested
    @DisplayName("should generate opml")
    inner class ShouldGenerateOPML {

        private val podcast1 = Podcast(
            id = UUID.fromString("ad16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast first",
            description = "desc",
            signature = null,
            url = "https://foo.bar.com/app/1.rss",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
            type = "RSS",
            tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

            cover = Cover(
                id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                url = URI("https://external.domain.tld/1.png"),
                height = 200, width = 200
            )
        )
        private val podcast2 = Podcast(
            id = UUID.fromString("bd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast second",
            description = "desc",
            signature = null,
            url = "https://foo.bar.com/app/2.rss",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
            type = "RSS",
            tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

            cover = Cover(
                id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                url = URI("https://external.domain.tld/2.png"),
                height = 200, width = 200
            )
        )
        private val podcast3 = Podcast(
            id = UUID.fromString("cd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast third",
            description = "desc",
            signature = null,
            url = "https://foo.bar.com/app/3.rss",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
            type = "RSS",
            tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

            cover = Cover(
                id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                url = URI("https://external.domain.tld/3.png"),
                height = 200, width = 200
            )
        )

        @Test
        fun `with no podcast`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(Flux.empty())

            /* When */
            rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/opml")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body />
                        </opml>
                    """.trimIndent())
        }

        @Test
        fun `with one podcast`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(listOf(podcast1).toFlux())

            /* When */
            rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/opml")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body>
                            <outline text="Podcast first" description="desc" htmlUrl="https://localhost:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" title="Podcast first" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" />
                          </body>
                        </opml>
                    """.trimIndent())
        }

        @Test
        fun `with 3 podcasts`() {
            /* Given */
            whenever(podcastService.findAll()).thenReturn(listOf(podcast1, podcast2, podcast3).toFlux())

            /* When */
            rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/opml")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body>
                            <outline text="Podcast first" description="desc" htmlUrl="https://localhost:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" title="Podcast first" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" />
                            <outline text="Podcast second" description="desc" htmlUrl="https://localhost:8080/podcasts/bd16b2eb-657e-4064-b470-5b99397ce729" title="Podcast second" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/bd16b2eb-657e-4064-b470-5b99397ce729/rss" />
                            <outline text="Podcast third" description="desc" htmlUrl="https://localhost:8080/podcasts/cd16b2eb-657e-4064-b470-5b99397ce729" title="Podcast third" type="rss" version="RSS2" xmlUrl="https://localhost:8080/api/podcasts/cd16b2eb-657e-4064-b470-5b99397ce729/rss" />
                          </body>
                        </opml>
                    """.trimIndent())
        }

        @Nested
        @DisplayName("with variable header configuration as host")
        inner class WithVariableHeaderConfigurationAsHost {

            @BeforeEach
            fun beforeEach() {
                whenever(podcastService.findAll()).thenReturn(listOf(podcast1).toFlux())
            }

            @Test
            fun `with no header configuration, everything comes from url`() {
                /* Given */
                /* When */
                rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/opml")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .xml("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <opml version="2.0">
                          <head>
                            <title>Podcast-Server</title>
                          </head>
                          <body>
                            <outline htmlUrl="https://localhost:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" 
                                        text="Podcast first" description="desc" 
                                        title="Podcast first" type="rss" version="RSS2" 
                                        xmlUrl="https://localhost:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" 
                                    />
                          </body>
                        </opml>
                    """.trimIndent())
            }

            @Nested
            @DisplayName("on host")
            inner class OnHost {

                @Test
                fun `with Host header coming from the load balancer`() {
                    /* Given */

                    /* When */
                    rest
                        .get()
                        .uri("https://localhost:8080/api/v1/podcasts/opml")
                        .header("Host", "custom-host")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .xml("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <opml version="2.0">
                                  <head>
                                    <title>Podcast-Server</title>
                                  </head>
                                  <body>
                                    <outline htmlUrl="https://custom-host:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" 
                                        text="Podcast first" description="desc" 
                                        title="Podcast first" type="rss" version="RSS2" 
                                        xmlUrl="https://custom-host:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" 
                                    />
                                  </body>
                                </opml>
                            """.trimIndent())
                }

                @Test
                fun `with X-Forwarded-Host header coming from the load balancer`() {
                    /* Given */

                    /* When */
                    rest
                        .get()
                        .uri("https://localhost:8080/api/v1/podcasts/opml")
                        .header("X-Forwarded-Host", "custom-host")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .xml("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <opml version="2.0">
                                  <head>
                                    <title>Podcast-Server</title>
                                  </head>
                                  <body>
                                    <outline htmlUrl="https://custom-host:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" 
                                        text="Podcast first" description="desc" 
                                        title="Podcast first" type="rss" version="RSS2" 
                                        xmlUrl="https://custom-host:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" 
                                    />
                                  </body>
                                </opml>
                            """.trimIndent())
                }
            }

            @Nested
            @DisplayName("on scheme")
            inner class OnScheme {
                @Test
                fun `with X-Forwarded-Proto header coming from the load balancer`() {
                    /* Given */

                    /* When */
                    rest
                        .get()
                        .uri("https://localhost:8080/api/v1/podcasts/opml")
                        .header("X-Forwarded-Proto", "http")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .xml("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <opml version="2.0">
                                  <head>
                                    <title>Podcast-Server</title>
                                  </head>
                                  <body>
                                    <outline htmlUrl="http://localhost:8080/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" 
                                        text="Podcast first" description="desc" 
                                        title="Podcast first" type="rss" version="RSS2" 
                                        xmlUrl="http://localhost:8080/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" 
                                    />
                                  </body>
                                </opml>
                            """.trimIndent())
                }
            }

            @Nested
            @DisplayName("on port")
            inner class OnPort {

                @Test
                fun `with X-Forwarded-Port header coming from the load balancer`() {
                    /* Given */

                    /* When */
                    rest
                        .get()
                        .uri("https://localhost:8080/api/v1/podcasts/opml")
                        .header("X-Forwarded-Port", "9876")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody()
                        .xml("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <opml version="2.0">
                                  <head>
                                    <title>Podcast-Server</title>
                                  </head>
                                  <body>
                                    <outline htmlUrl="https://localhost:9876/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729" 
                                        text="Podcast first" description="desc" 
                                        title="Podcast first" type="rss" version="RSS2" 
                                        xmlUrl="https://localhost:9876/api/podcasts/ad16b2eb-657e-4064-b470-5b99397ce729/rss" 
                                    />
                                  </body>
                                </opml>
                            """.trimIndent())
                }

            }

        }

    }

    @Nested
    @DisplayName("should generate rss")
    inner class ShouldGenerateRss {

        private val coverForItem = Item.Cover(
            id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
            url = URI("https://external.domain.tld/foo/bar.png"),
            width = 200,
            height = 200
        )

        private val podcastForItem = Item.Podcast(
            id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast title",
            url = "https://foo.bar.com/app/file.rss"
        )

        private val items = (200 downTo 1)
            .map { it.toString().padStart(3, '0') }
            .map { Item(
                id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f$it"),
                title = "Foo $it",
                url = "https://external.domain.tld/foo/bar.$it.mp4",

                pubDate = OffsetDateTime.of(2019, 6, 24, 5, 28, 54, 34, ZoneOffset.ofHours(2)).minusDays(200 - it.toLong() ),
                creationDate = OffsetDateTime.of(2019, 6, 24, 5, 29, 54, 34, ZoneOffset.ofHours(2)).minusDays(200 - it.toLong()),
                downloadDate = OffsetDateTime.of(2019, 6, 25, 5, 30, 54, 34, ZoneOffset.ofHours(2)).minusDays(200 - it.toLong()),

                description = "desc $it",
                mimeType = "video/mp4",
                length = 100,
                fileName = null,
                status = Status.NOT_DOWNLOADED,

                podcast = podcastForItem,
                cover = coverForItem
            ) }

        @Test
        fun `for podcast with limit`() {
            /* Given */
            val podcastId = podcastForItem.id
            val size = 50
            val page = ItemPageRequest(0, size, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(items.take(size), size, page)

            whenever(itemService.search(anyOrNull(), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                .thenReturn(result.toMono())
            whenever(podcastService.findById(podcastId))
                .thenReturn(podcast.toMono())

            val xml = fileAsString("/xml/podcast-with-50-items.xml")

            /* When */
            rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/$podcastId/rss")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .xml(xml.trimIndent())
        }

        @Test
        fun `for podcast without limit`() {
            /* Given */
            val podcastId = podcastForItem.id
            val page = ItemPageRequest(0, Int.MAX_VALUE, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(items.take(200), 200, page)

            whenever(itemService.search(anyOrNull(), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                .thenReturn(result.toMono())
            whenever(podcastService.findById(podcastId))
                .thenReturn(podcast.toMono())

            val xml = fileAsString("/xml/podcast-with-200-items.xml")

            /* When */
            rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/$podcastId/rss?limit=false")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .xml(xml.trimIndent())
        }

        @Test
        fun `for podcast with amy parameters`() {
            /* Given */
            val podcastId = podcastForItem.id
            val page = ItemPageRequest(0, Int.MAX_VALUE, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(emptyList(), 0, page)

            whenever(itemService.search(anyOrNull(), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                .thenReturn(result.toMono())
            whenever(podcastService.findById(podcastId)).thenReturn(podcast.toMono())

            val xml = fileAsString("/xml/podcast-with-lots-of-parameters.xml")

            /* When */
            rest
                .get()
                .uri("https://localhost:8080/api/v1/podcasts/$podcastId/rss?v=1234&limit=false")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .xml(xml.trimIndent())
        }
    }
}

