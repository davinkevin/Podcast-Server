package com.github.davinkevin.podcastserver.digest

import com.github.davinkevin.podcastserver.config.JacksonConfig
import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.extension.mockmvc.MockMvcRestExceptionConfiguration
import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import com.github.davinkevin.podcastserver.item.Item
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

private val fixedDate = Clock.fixed(
    OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC).toInstant(),
    ZoneId.of("UTC"),
)

@NestedSpringTest
@WebMvcTest(controllers = [DigestHandler::class])
@Import(DigestRoutingConfig::class, JacksonConfig::class, MockMvcRestExceptionConfiguration::class, DigestHandlerTest.LocalTestConfiguration::class)
@AutoConfigureWebTestClient
class DigestHandlerTest(
    @Autowired val rest: WebTestClient,
) {

    @MockitoBean private lateinit var digestService: DigestService

    private val podcastId = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9")
    private val itemId = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c")

    private val item = Item(
        id = itemId,
        title = "Foo",
        url = "https://external.domain.tld/foo/bar.mp4",
        pubDate = OffsetDateTime.of(2019, 3, 1, 13, 14, 15, 0, ZoneOffset.UTC),
        downloadDate = null,
        creationDate = OffsetDateTime.of(2019, 2, 5, 13, 14, 15, 0, ZoneOffset.UTC),
        description = "desc",
        mimeType = "audio/mp3",
        length = 100,
        fileName = null,
        status = Status.NOT_DOWNLOADED,
        podcast = Item.Podcast(podcastId, "Podcast Bar", null),
        cover = Item.Cover(
            id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
            url = URI("https://external.domain.tld/foo/bar.png"),
            width = 200,
            height = 200,
        ),
    )

    private val digestPodcast = DigestPodcast(
        id = podcastId,
        title = "Podcast Bar",
        cover = Cover(
            id = UUID.fromString("11111111-2222-3333-4444-555555555555"),
            url = URI("https://external.domain.tld/podcast/bar.png"),
            height = 400,
            width = 400,
        ),
        itemCount = 3,
        items = listOf(item),
    )

    @Nested
    @DisplayName("should find digest")
    inner class ShouldFindDigest {

        @Test
        fun `with default window of 7 days and 12 items per podcast`() {
            whenever(digestService.findActivePodcastsSince(any(), any()))
                .thenReturn(listOf(digestPodcast))

            rest.get()
                .uri("/api/v1/digest")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .assertThatJson {
                    isEqualTo(""" {
                        "content": [
                            {
                                "id": "8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                                "title": "Podcast Bar",
                                "itemCount": 3,
                                "cover": {
                                    "id": "11111111-2222-3333-4444-555555555555",
                                    "width": 400,
                                    "height": 400,
                                    "url": "https://external.domain.tld/podcast/bar.png",
                                    "proxyURL": "/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/cover.png"
                                },
                                "items": [
                                    {
                                        "id": "27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                                        "title": "Foo",
                                        "url": "https://external.domain.tld/foo/bar.mp4",
                                        "pubDate": "2019-03-01T13:14:15Z",
                                        "downloadDate": null,
                                        "creationDate": "2019-02-05T13:14:15Z",
                                        "description": "desc",
                                        "mimeType": "audio/mp3",
                                        "length": 100,
                                        "fileName": null,
                                        "status": "NOT_DOWNLOADED",
                                        "podcastId": "8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                                        "isDownloaded": false,
                                        "proxyURL": "/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo",
                                        "podcast": {
                                            "id": "8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                                            "title": "Podcast Bar",
                                            "url": null
                                        },
                                        "cover": {
                                            "id": "f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                                            "width": 200,
                                            "height": 200,
                                            "url": "https://external.domain.tld/foo/bar.png",
                                            "proxyURL": "/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png"
                                        }
                                    }
                                ]
                            }
                        ]
                    } """)
                }

            verify(digestService).findActivePodcastsSince(
                eq(OffsetDateTime.of(2019, 2, 25, 5, 6, 7, 0, ZoneOffset.UTC)),
                eq(12),
            )
        }

        @Test
        fun `honoring within and maxItemsPerPodcast query params`() {
            whenever(digestService.findActivePodcastsSince(any(), any()))
                .thenReturn(emptyList())

            rest.get()
                .uri("/api/v1/digest?within=PT24H&maxItemsPerPodcast=5")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .assertThatJson { isEqualTo("""{ "content": [] }""") }

            verify(digestService).findActivePodcastsSince(
                eq(OffsetDateTime.of(2019, 3, 3, 5, 6, 7, 0, ZoneOffset.UTC)),
                eq(5),
            )
        }
    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun fixedClock(): Clock = Clock.fixed(fixedDate.instant(), ZoneId.of("UTC"))
    }
}
