package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.service.FileService
import com.nhaarman.mockitokotlin2.*
import org.apache.commons.io.FilenameUtils
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/**
 * Created by kevin on 2019-02-12
 */
@WebFluxTest(controllers = [ItemHandler::class])
@Import(ItemRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class ItemHandlerTest(
    @Autowired val rest: WebTestClient,
    @Autowired val clock: Clock
) {

    @MockBean private lateinit var itemService: ItemService
    @MockBean private lateinit var fileService: FileService

    val item = Item(
            id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
            title = "Foo",
            url = "https://external.domain.tld/foo/bar.mp4",

            pubDate = now(),
            downloadDate = now(),
            creationDate = now(),

            description = "desc",
            mimeType = "audio/mp3",
            length = 100,
            fileName = null,
            status = Status.NOT_DOWNLOADED,

            podcast = Item.Podcast(
                    id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                    title = "Podcast Bar",
                    url = "https://external.domain.tld/bar.rss"
            ),
            cover = Item.Cover(
                    id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                    url = URI("https://external.domain.tld/foo/bar.png"),
                    width = 200,
                    height = 200
            )
    )

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @Test
        fun `with the default number of days to keep because no parameters`() {
            /* Given */
            whenever(itemService.deleteItemOlderThan(fixedDate.minusDays(30))).thenReturn(Mono.empty())

            /* When */
            rest.delete()
                    .uri("/api/v1/items")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
        }

        @Test
        fun `with number of days to keep on the url query params`() {
            /* Given */
            whenever(itemService.deleteItemOlderThan(fixedDate.minusDays(60))).thenReturn(Mono.empty())

            /* When */
            rest.delete()
                    .uri("/api/v1/items?days=60")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
        }
    }

    @Nested
    @DisplayName("should serve file")
    inner class ShouldServeFile {

        @Test
        fun `by redirecting to local file server`() {
            /* Given */
            val itemDownloaded = item.copy(
                    status = Status.FINISH, fileName = "file_to_download.mp4"
            )
            whenever(itemService.findById(item.id)).thenReturn(itemDownloaded.toMono())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{idPodcast}/items/{id}/{file}", item.podcast.id, item.id, "download.mp4")
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://localhost:8080/data/Podcast%20Bar/file_to_download.mp4")
        }

        @Test
        fun `by redirecting if element is not downloaded`() {
            /* Given */
            whenever(itemService.findById(item.id)).thenReturn(item.toMono())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/{idPodcast}/items/{id}/{file}", item.podcast.id, item.id, "download.mp4")
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://external.domain.tld/foo/bar.mp4")
        }

        @Test
        fun `and throw 404 if nothing is found`() {
            /* Given */
            whenever(itemService.findById(item.id)).thenReturn(Mono.empty())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/{idPodcast}/items/{id}/{file}", item.podcast.id, item.id, "download.mp4")
                    .exchange()
                    /* Then */
                    .expectStatus().isNotFound
                    .expectBody()
                    .assertThatJson {
                        inPath("status").isEqualTo(404)
                        inPath("message").isEqualTo("No item found for id ${item.id}")
                    }

        }
    }

    @Nested
    @DisplayName("should serve cover")
    inner class ShouldServerCover {

        @Test
        fun `by redirecting to local file server if cover exists locally`() {
            /* Given */
            whenever(itemService.findById(item.id)).thenReturn(item.toMono())
            whenever(fileService.coverExists(any<Item>())).then {
                val item = it.getArgument<Item>(0)
                val extension = FilenameUtils.getExtension(item.cover.url.toASCIIString())
                "${item.id}.$extension".toMono()
            }

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{idPodcast}/items/{id}/cover.{ext}", item.podcast.id, item.id, "png")
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://localhost:8080/data/Podcast%20Bar/27184b1a-7642-4ffd-ac7e-14fb36f7f15c.png")
        }

        @Test
        fun `by redirecting to external file if cover does not exist locally`() {
            /* Given */
            whenever(itemService.findById(item.id)).thenReturn(item.toMono())
            whenever(fileService.coverExists(any<Item>())).then { Mono.empty<Path>() }

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/{idPodcast}/items/{id}/cover.{ext}", item.podcast.id, item.id, "png")
                    .exchange()
                    /* Then */
                    .expectStatus().isSeeOther
                    .expectHeader()
                    .valueEquals("Location", "https://external.domain.tld/foo/bar.png")
        }
    }

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

        private val notDownloadedItem = Item(
                id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
                title = "Foo",
                url = "https://external.domain.tld/foo/bar.mp4",

                pubDate = OffsetDateTime.parse("2019-02-01T13:14:15.000Z"),
                downloadDate = null,
                creationDate = OffsetDateTime.parse("2019-02-05T13:14:15.000Z"),

                description = "desc",
                mimeType = "audio/mp3",
                length = 100,
                fileName = null,
                status = Status.NOT_DOWNLOADED,

                podcast = Item.Podcast(
                        id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                        title = "Podcast Bar",
                        url = "https://external.domain.tld/bar.rss"
                ),
                cover = Item.Cover(
                        id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                        url = URI("https://external.domain.tld/foo/bar.png"),
                        width = 200,
                        height = 200
                )
        )

        @Test
        fun `with not downloaded item`() {
            /* Given */
            val pid = notDownloadedItem.podcast.id
            val iid = notDownloadedItem.id
            whenever(itemService.findById(iid)).thenReturn(notDownloadedItem.toMono())
            /* When */
            rest.get()
                    .uri("/api/v1/podcasts/{pid}/items/{iid}", pid, iid)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo(""" {
                           "cover":{
                              "height":200,
                              "id":"f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                              "url":"/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png",
                              "width":200
                           },
                           "creationDate":"2019-02-05T13:14:15Z",
                           "description":"desc",
                           "downloadDate":null,
                           "fileName":null,
                           "id":"27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                           "isDownloaded":false,
                           "length":100,
                           "mimeType":"audio/mp3",
                           "podcast":{
                              "id":"8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                              "title":"Podcast Bar",
                              "url":"https://external.domain.tld/bar.rss"
                           },
                           "podcastId":"8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                           "proxyURL":"/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo",
                           "pubDate":"2019-02-01T13:14:15Z",
                           "status":"NOT_DOWNLOADED",
                           "title":"Foo",
                           "url":"https://external.domain.tld/foo/bar.mp4"
                        } """)
                    }
        }

        @Test
        fun `with downloaded item`() {
            /* Given */
            val downloadedItem = notDownloadedItem.copy(
                    fileName = "foo.mp4",
                    mimeType = "video/mp4",
                    status = Status.FINISH
            )
            val pid = downloadedItem.podcast.id
            val iid = downloadedItem.id
            whenever(itemService.findById(iid)).thenReturn(downloadedItem.toMono())
            /* When */
            rest.get()
                    .uri("/api/v1/podcasts/{pid}/items/{iid}", pid, iid)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo(""" {
                           "cover":{
                              "height":200,
                              "id":"f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                              "url":"/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png",
                              "width":200
                           },
                           "creationDate":"2019-02-05T13:14:15Z",
                           "description":"desc",
                           "downloadDate":null,
                           "fileName":"foo.mp4",
                           "id":"27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                           "isDownloaded":true,
                           "length":100,
                           "mimeType":"video/mp4",
                           "podcast":{
                              "id":"8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                              "title":"Podcast Bar",
                              "url":"https://external.domain.tld/bar.rss"
                           },
                           "podcastId":"8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                           "proxyURL":"/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo.mp4",
                           "pubDate":"2019-02-01T13:14:15Z",
                           "status":"FINISH",
                           "title":"Foo",
                           "url":"https://external.domain.tld/foo/bar.mp4"
                        } """)
                    }
        }


    }

    @Nested
    @DisplayName("should reset")
    inner class ShouldReset {

        private val anItemToBeReseted = Item(
                id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
                title = "Foo",
                url = "https://external.domain.tld/foo/bar.mp4",

                pubDate = OffsetDateTime.parse("2019-02-01T13:14:15.000Z"),
                downloadDate = null,
                creationDate = OffsetDateTime.parse("2019-02-05T13:14:15.000Z"),

                description = "desc",
                mimeType = "audio/mp3",
                length = 100,
                fileName = null,
                status = Status.NOT_DOWNLOADED,

                podcast = Item.Podcast(
                        id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                        title = "Podcast Bar",
                        url = "https://external.domain.tld/bar.rss"
                ),
                cover = Item.Cover(
                        id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                        url = URI("https://external.domain.tld/foo/bar.png"),
                        width = 200,
                        height = 200
                )
        )


        @Test
        fun `an item`() {
            /* Given */
            whenever(itemService.reset(anItemToBeReseted.id)).thenReturn(anItemToBeReseted.toMono())
            /* When */
            rest.post()
                    .uri("/api/v1/podcasts/${anItemToBeReseted.podcast.id}/items/${anItemToBeReseted.id}/reset")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo(""" {
                           "cover":{
                              "height":200,
                              "id":"f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                              "url":"/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png",
                              "width":200
                           },
                           "creationDate":"2019-02-05T13:14:15Z",
                           "description":"desc",
                           "downloadDate":null,
                           "fileName":null,
                           "id":"27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                           "isDownloaded":false,
                           "length":100,
                           "mimeType":"audio/mp3",
                           "podcast":{
                              "id":"8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                              "title":"Podcast Bar",
                              "url":"https://external.domain.tld/bar.rss"
                           },
                           "podcastId":"8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9",
                           "proxyURL":"/api/v1/podcasts/8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo",
                           "pubDate":"2019-02-01T13:14:15Z",
                           "status":"NOT_DOWNLOADED",
                           "title":"Foo",
                           "url":"https://external.domain.tld/foo/bar.mp4"
                        } """)
                    }
        }
    }

    @Nested
    @DisplayName("should search for item")
    inner class ShouldSearchForItem {

        @Nested
        @DisplayName("on parameters")
        inner class OnParameters {

            val zeroResult = PageItem(
                    content = listOf(),
                    empty = true,
                    first = true,
                    last = true,
                    number = 0,
                    numberOfElements = 0,
                    size = 0,
                    totalElements = 0,
                    totalPages = 0
            )

            @Test
            fun `with default values`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with query parameter`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("foo", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?q=foo")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with no tag`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("http://localhost:8080/api/v1/items/search?tags=")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with one tag`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", listOf("foo"), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?tags=foo")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with multiple tags`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", listOf("foo", "bar"), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?tags=foo,bar")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with some tags empty`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", listOf("foo", "bar"), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("http://localhost:8080/api/v1/items/search?tags=foo,bar,,,")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }


            @Test
            fun `with no status`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?status=")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with one status`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), listOf(Status.STARTED), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?status=STARTED")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with multiple status`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), listOf(Status.STARTED, Status.DELETED), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?status=STARTED,DELETED")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with some status empty`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), listOf(Status.STARTED, Status.DELETED), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?status=STARTED,DELETED,,,")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with page parameter`() {
                /* Given */
                val request = ItemPageRequest(1, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?page=1")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with size parameter`() {
                /* Given */
                val request = ItemPageRequest(0, 24, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?size=24")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }

            @Test
            fun `with sort parameter`() {
                /* Given */
                val request = ItemPageRequest(0, 12, ItemSort("ASC", "downloadDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(zeroResult.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search?sort=downloadDate,ASC")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }


        }

        @Nested
        @DisplayName("withSuccess")
        inner class WithSuccess {

            private val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))

            @Test
            fun `with no items`() {
                /* Given */
                val result = PageItem(
                        content = listOf(),
                        empty = true,
                        first = true,
                        last = true,
                        number = 0,
                        numberOfElements = 0,
                        size = 0,
                        totalElements = 0,
                        totalPages = 0
                )

                val request = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(result.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {isEqualTo("""{
                               "content":[],
                               "empty":true,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":0,
                               "size":0,
                               "totalElements":0,
                               "totalPages":0
                            }""")
                        }
            }
            private val podcast = Item.Podcast(UUID.fromString("ef62c5c3-e79f-4474-8228-40b76abcdb57"), "podcast 1", "https/foo.bar.com/rss")

            private val item1 = Item(
                    id = UUID.fromString("6a287582-e181-48f9-a23d-c88d03879feb"),
                    title = "item 1",
                    url = "https://foo.bar.com/1.mp3",

                    pubDate = now(clock),
                    downloadDate = now(clock),
                    creationDate = now(clock),

                    description = "item 1 desc",
                    mimeType = "audio/mp3",
                    length = 1234,
                    fileName = "1.mp3",
                    status = Status.FINISH,

                    podcast = podcast,
                    cover = Item.Cover(UUID.fromString("337edcd5-97d3-4f78-9a5b-1c14c999883b"), URI("https://foo.bar.com/cover.png"), 100, 100)
            )
            private val item2 = Item(
                    id = UUID.fromString("cb4cd9b9-957a-457e-a72a-519241c51aca"),
                    title = "item 2",
                    url = "https://foo.bar.com/2.mp3",

                    pubDate = now(clock),
                    downloadDate = now(clock),
                    creationDate = now(clock),

                    description = "item 2 desc",
                    mimeType = "audio/mp3",
                    length = 1234,
                    fileName = "2.mp3",
                    status = Status.FINISH,

                    podcast = podcast,
                    cover = Item.Cover(UUID.fromString("319072db-4411-4975-884e-f1cacbfb1471"), URI("https://foo.bar.com/2/cover.png"), 100, 100)
            )
            private val item3 = Item(
                    id = UUID.fromString("ffbeb6b9-0e1f-4fb1-ab95-0f92effcc621"),
                    title = "item 3",
                    url = "https://foo.bar.com/3.mp3",

                    pubDate = now(clock),
                    downloadDate = now(clock),
                    creationDate = now(clock),

                    description = "item 3 desc",
                    mimeType = "audio/mp3",
                    length = 1234,
                    fileName = "3.mp3",
                    status = Status.FINISH,

                    podcast = podcast,
                    cover = Item.Cover(UUID.fromString("388d596e-858a-4746-bbe7-a1c027fa2fc4"), URI("https://foo.bar.com/3/cover.png"), 100, 100)
            )

            @Test
            fun `with one items`() {
                /* Given */
                val result = PageItem.of(listOf(item1), 1, request)
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(result.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {
                            isEqualTo("""{
                               "content":[
                                   {
                                      "cover":{
                                         "height":100,
                                         "id":"337edcd5-97d3-4f78-9a5b-1c14c999883b",
                                         "url":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/6a287582-e181-48f9-a23d-c88d03879feb/cover.png",
                                         "width":100
                                      },
                                      "creationDate":"2019-03-04T05:06:07Z",
                                      "description":"item 1 desc",
                                      "downloadDate":"2019-03-04T05:06:07Z",
                                      "fileName":"1.mp3",
                                      "id":"6a287582-e181-48f9-a23d-c88d03879feb",
                                      "isDownloaded":true,
                                      "length":1234,
                                      "mimeType":"audio/mp3",
                                      "podcast":{
                                         "id":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                         "title":"podcast 1",
                                         "url":"https/foo.bar.com/rss"
                                      },
                                      "podcastId":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                      "proxyURL":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/6a287582-e181-48f9-a23d-c88d03879feb/item_1.mp3",
                                      "pubDate":"2019-03-04T05:06:07Z",
                                      "status":"FINISH",
                                      "title":"item 1",
                                      "url":"https://foo.bar.com/1.mp3"
                                   }
                                ],
                               "empty":false,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":1,
                               "size":12,
                               "totalElements":1,
                               "totalPages":1
                            }""")
                        }
            }

            @Test
            fun `with two items`() {
                /* Given */
                val result = PageItem.of(listOf(item1, item2), 2, request)
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(result.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {
                            isEqualTo("""{
                               "content":[
                               {
                                  "cover":{
                                     "height":100,
                                     "id":"337edcd5-97d3-4f78-9a5b-1c14c999883b",
                                     "url":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/6a287582-e181-48f9-a23d-c88d03879feb/cover.png",
                                     "width":100
                                  },
                                  "creationDate":"2019-03-04T05:06:07Z",
                                  "description":"item 1 desc",
                                  "downloadDate":"2019-03-04T05:06:07Z",
                                  "fileName":"1.mp3",
                                  "id":"6a287582-e181-48f9-a23d-c88d03879feb",
                                  "isDownloaded":true,
                                  "length":1234,
                                  "mimeType":"audio/mp3",
                                  "podcast":{
                                     "id":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                     "title":"podcast 1",
                                     "url":"https/foo.bar.com/rss"
                                  },
                                  "podcastId":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                  "proxyURL":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/6a287582-e181-48f9-a23d-c88d03879feb/item_1.mp3",
                                  "pubDate":"2019-03-04T05:06:07Z",
                                  "status":"FINISH",
                                  "title":"item 1",
                                  "url":"https://foo.bar.com/1.mp3"
                               },
                               {
                                  "cover":{
                                     "height":100,
                                     "id":"319072db-4411-4975-884e-f1cacbfb1471",
                                     "url":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/cb4cd9b9-957a-457e-a72a-519241c51aca/cover.png",
                                     "width":100
                                  },
                                  "creationDate":"2019-03-04T05:06:07Z",
                                  "description":"item 2 desc",
                                  "downloadDate":"2019-03-04T05:06:07Z",
                                  "fileName":"2.mp3",
                                  "id":"cb4cd9b9-957a-457e-a72a-519241c51aca",
                                  "isDownloaded":true,
                                  "length":1234,
                                  "mimeType":"audio/mp3",
                                  "podcast":{
                                     "id":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                     "title":"podcast 1",
                                     "url":"https/foo.bar.com/rss"
                                  },
                                  "podcastId":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                  "proxyURL":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/cb4cd9b9-957a-457e-a72a-519241c51aca/item_2.mp3",
                                  "pubDate":"2019-03-04T05:06:07Z",
                                  "status":"FINISH",
                                  "title":"item 2",
                                  "url":"https://foo.bar.com/2.mp3"
                               }
                            ],
                               "empty":false,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":2,
                               "size":12,
                               "totalElements":2,
                               "totalPages":1
                            }""")
                        }
            }

            @Test
            fun `with three items`() {
                /* Given */
                val result = PageItem.of(listOf(item1, item2, item3), 3, request)
                whenever(itemService.search("", emptyList(), emptyList(), request)).thenReturn(result.toMono())
                /* When */
                rest
                        .get()
                        .uri("https://localhost:8080/api/v1/items/search")
                        .exchange()
                        /* Then */
                        .expectStatus().isOk
                        .expectBody().assertThatJson {
                            isEqualTo("""{
                               "content":[
                               {
                                  "cover":{
                                     "height":100,
                                     "id":"337edcd5-97d3-4f78-9a5b-1c14c999883b",
                                     "url":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/6a287582-e181-48f9-a23d-c88d03879feb/cover.png",
                                     "width":100
                                  },
                                  "creationDate":"2019-03-04T05:06:07Z",
                                  "description":"item 1 desc",
                                  "downloadDate":"2019-03-04T05:06:07Z",
                                  "fileName":"1.mp3",
                                  "id":"6a287582-e181-48f9-a23d-c88d03879feb",
                                  "isDownloaded":true,
                                  "length":1234,
                                  "mimeType":"audio/mp3",
                                  "podcast":{ "id":"ef62c5c3-e79f-4474-8228-40b76abcdb57", "title":"podcast 1", "url":"https/foo.bar.com/rss" },
                                  "podcastId":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                  "proxyURL":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/6a287582-e181-48f9-a23d-c88d03879feb/item_1.mp3",
                                  "pubDate":"2019-03-04T05:06:07Z",
                                  "status":"FINISH",
                                  "title":"item 1",
                                  "url":"https://foo.bar.com/1.mp3"
                               },
                               {
                                  "cover":{
                                     "height":100,
                                     "id":"319072db-4411-4975-884e-f1cacbfb1471",
                                     "url":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/cb4cd9b9-957a-457e-a72a-519241c51aca/cover.png",
                                     "width":100
                                  },
                                  "creationDate":"2019-03-04T05:06:07Z",
                                  "description":"item 2 desc",
                                  "downloadDate":"2019-03-04T05:06:07Z",
                                  "fileName":"2.mp3",
                                  "id":"cb4cd9b9-957a-457e-a72a-519241c51aca",
                                  "isDownloaded":true,
                                  "length":1234,
                                  "mimeType":"audio/mp3",
                                  "podcast":{ "id":"ef62c5c3-e79f-4474-8228-40b76abcdb57", "title":"podcast 1", "url":"https/foo.bar.com/rss" },
                                  "podcastId":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                  "proxyURL":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/cb4cd9b9-957a-457e-a72a-519241c51aca/item_2.mp3",
                                  "pubDate":"2019-03-04T05:06:07Z",
                                  "status":"FINISH",
                                  "title":"item 2",
                                  "url":"https://foo.bar.com/2.mp3"
                               },
                               {
                                  "cover":{
                                     "height":100,
                                     "id":"388d596e-858a-4746-bbe7-a1c027fa2fc4",
                                     "url":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/ffbeb6b9-0e1f-4fb1-ab95-0f92effcc621/cover.png",
                                     "width":100
                                  },
                                  "creationDate":"2019-03-04T05:06:07Z",
                                  "description":"item 3 desc",
                                  "downloadDate":"2019-03-04T05:06:07Z",
                                  "fileName":"3.mp3",
                                  "id":"ffbeb6b9-0e1f-4fb1-ab95-0f92effcc621",
                                  "isDownloaded":true,
                                  "length":1234,
                                  "mimeType":"audio/mp3",
                                  "podcast":{ "id":"ef62c5c3-e79f-4474-8228-40b76abcdb57", "title":"podcast 1", "url":"https/foo.bar.com/rss" },
                                  "podcastId":"ef62c5c3-e79f-4474-8228-40b76abcdb57",
                                  "proxyURL":"/api/v1/podcasts/ef62c5c3-e79f-4474-8228-40b76abcdb57/items/ffbeb6b9-0e1f-4fb1-ab95-0f92effcc621/item_3.mp3",
                                  "pubDate":"2019-03-04T05:06:07Z",
                                  "status":"FINISH",
                                  "title":"item 3",
                                  "url":"https://foo.bar.com/3.mp3"
                               }
                            ],
                               "empty":false,
                               "first":true,
                               "last":true,
                               "number":0,
                               "numberOfElements":3,
                               "size":12,
                               "totalElements":3,
                               "totalPages":1
                            }""")
                        }
            }


        }

    }

    @Nested
    @DisplayName("should find items of podcast")
    inner class ShouldFindItemsOfPodcast {

        private val item = Item(
                id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
                title = "Foo",
                url = "https://external.domain.tld/foo/bar.mp4",

                pubDate = OffsetDateTime.of(2019, 6, 24, 5, 28, 54, 34, ZoneOffset.ofHours(2)),
                creationDate = OffsetDateTime.of(2019, 6, 24, 5, 29, 54, 34, ZoneOffset.ofHours(2)),
                downloadDate = OffsetDateTime.of(2019, 6, 25, 5, 30, 54, 34, ZoneOffset.ofHours(2)),

                description = "desc",
                mimeType = "audio/mp3",
                length = 100,
                fileName = null,
                status = Status.NOT_DOWNLOADED,

                podcast = Item.Podcast(
                        id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
                        title = "Podcast title",
                        url = "https://foo.bar.com/app/file.rss"
                ),
                cover = Item.Cover(
                        id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                        url = URI("https://external.domain.tld/foo/bar.png"),
                        width = 200,
                        height = 200
                )
        )

        @Test
        fun `should find by podcast with standard parameters`() {
            /* Given */
            val podcastId = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729")
            val page = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(listOf(item), 1, page)
            whenever(itemService.search(anyOrNull(), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                    .thenReturn(result.toMono())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/$podcastId/items?page=0&size=12&sort=pubDate,DESC")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                          "content": [
                            {
                              "cover": {
                                "height": 200,
                                "id": "f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                                "url": "/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png",
                                "width": 200
                              },
                              "creationDate": "2019-06-24T05:29:54.000000034+02:00",
                              "description": "desc",
                              "downloadDate": "2019-06-25T05:30:54.000000034+02:00",
                              "fileName": null,
                              "id": "27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                              "isDownloaded": false,
                              "length": 100,
                              "mimeType": "audio/mp3",
                              "podcast": {
                                "id": "dd16b2eb-657e-4064-b470-5b99397ce729",
                                "title": "Podcast title",
                                "url": "https://foo.bar.com/app/file.rss"
                              },
                              "podcastId": "dd16b2eb-657e-4064-b470-5b99397ce729",
                              "proxyURL": "/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo",
                              "pubDate": "2019-06-24T05:28:54.000000034+02:00",
                              "status": "NOT_DOWNLOADED",
                              "title": "Foo",
                              "url": "https://external.domain.tld/foo/bar.mp4"
                            }
                          ],
                          "empty": false,
                          "first": true,
                          "last": true,
                          "number": 0,
                          "numberOfElements": 1,
                          "size": 12,
                          "totalElements": 1,
                          "totalPages": 1
                        }""")
                    }
        }

        @Test
        fun `should find by podcast with tags`() {
            /* Given */
            val podcastId = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729")
            val page = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(listOf(item), 1, page)
            whenever(itemService.search(anyOrNull(), eq(listOf("foo", "bar")), eq(listOf()), eq(page), eq(podcastId)))
                    .thenReturn(result.toMono())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/$podcastId/items?page=0&size=12&sort=pubDate,DESC&tags=foo,bar")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                          "content": [
                            {
                              "cover": {
                                "height": 200,
                                "id": "f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                                "url": "/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png",
                                "width": 200
                              },
                              "creationDate": "2019-06-24T05:29:54.000000034+02:00",
                              "description": "desc",
                              "downloadDate": "2019-06-25T05:30:54.000000034+02:00",
                              "fileName": null,
                              "id": "27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                              "isDownloaded": false,
                              "length": 100,
                              "mimeType": "audio/mp3",
                              "podcast": {
                                "id": "dd16b2eb-657e-4064-b470-5b99397ce729",
                                "title": "Podcast title",
                                "url": "https://foo.bar.com/app/file.rss"
                              },
                              "podcastId": "dd16b2eb-657e-4064-b470-5b99397ce729",
                              "proxyURL": "/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo",
                              "pubDate": "2019-06-24T05:28:54.000000034+02:00",
                              "status": "NOT_DOWNLOADED",
                              "title": "Foo",
                              "url": "https://external.domain.tld/foo/bar.mp4"
                            }
                          ],
                          "empty": false,
                          "first": true,
                          "last": true,
                          "number": 0,
                          "numberOfElements": 1,
                          "size": 12,
                          "totalElements": 1,
                          "totalPages": 1
                        }""")
                    }
        }

        @Test
        fun `should find by podcast with query parameter`() {
            /* Given */
            val podcastId = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729")
            val page = ItemPageRequest(0, 12, ItemSort("DESC", "pubDate"))
            val result = PageItem.of(listOf(item), 1, page)
            whenever(itemService.search(eq("foo"), eq(listOf()), eq(listOf()), eq(page), eq(podcastId)))
                    .thenReturn(result.toMono())

            /* When */
            rest
                    .get()
                    .uri("https://localhost:8080/api/v1/podcasts/$podcastId/items?page=0&size=12&sort=pubDate,DESC&q=foo")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                          "content": [
                            {
                              "cover": {
                                "height": 200,
                                "id": "f4efe8db-7abf-4998-b15c-9fa2e06096a1",
                                "url": "/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/cover.png",
                                "width": 200
                              },
                              "creationDate": "2019-06-24T05:29:54.000000034+02:00",
                              "description": "desc",
                              "downloadDate": "2019-06-25T05:30:54.000000034+02:00",
                              "fileName": null,
                              "id": "27184b1a-7642-4ffd-ac7e-14fb36f7f15c",
                              "isDownloaded": false,
                              "length": 100,
                              "mimeType": "audio/mp3",
                              "podcast": {
                                "id": "dd16b2eb-657e-4064-b470-5b99397ce729",
                                "title": "Podcast title",
                                "url": "https://foo.bar.com/app/file.rss"
                              },
                              "podcastId": "dd16b2eb-657e-4064-b470-5b99397ce729",
                              "proxyURL": "/api/v1/podcasts/dd16b2eb-657e-4064-b470-5b99397ce729/items/27184b1a-7642-4ffd-ac7e-14fb36f7f15c/Foo",
                              "pubDate": "2019-06-24T05:28:54.000000034+02:00",
                              "status": "NOT_DOWNLOADED",
                              "title": "Foo",
                              "url": "https://external.domain.tld/foo/bar.mp4"
                            }
                          ],
                          "empty": false,
                          "first": true,
                          "last": true,
                          "number": 0,
                          "numberOfElements": 1,
                          "size": 12,
                          "totalElements": 1,
                          "totalPages": 1
                        }""")
                    }
        }
    }

    @Nested
    @DisplayName("should find watchLists associated to an item")
    inner class ShouldFindWatchListsAssociatedToAnItem {

        val item = Item(
                id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
                title = "Foo",
                url = "https://external.domain.tld/foo/bar.mp4",

                pubDate = now(),
                downloadDate = now(),
                creationDate = now(),

                description = "desc",
                mimeType = "audio/mp3",
                length = 100,
                fileName = null,
                status = Status.NOT_DOWNLOADED,

                podcast = Item.Podcast(
                        id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                        title = "Podcast Bar",
                        url = "https://external.domain.tld/bar.rss"
                ),
                cover = Item.Cover(
                        id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                        url = URI("https://external.domain.tld/foo/bar.png"),
                        width = 200,
                        height = 200
                )
        )

        @Test
        fun `with no watch list associated to this item`() {
            /* Given */
            whenever(itemService.findPlaylistsContainingItem(item.id))
                    .thenReturn(Flux.empty())

            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/{podcastId}/items/{id}/playlists", item.podcast.id, item.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                          "content": []
                        }""")
                    }
        }

        @Test
        fun `with 3 playlists associated to this item`() {
            /* Given */
            whenever(itemService.findPlaylistsContainingItem(item.id)).thenReturn(Flux.just(
                    ItemPlaylist(UUID.fromString("50958264-d5ed-4a9a-a875-5173bb207720"), "foo"),
                    ItemPlaylist(UUID.fromString("e053b63c-dc1d-4a3a-9c95-8f616a74d2aa"), "bar"),
                    ItemPlaylist(UUID.fromString("6761208b-85e7-4098-817a-2db7c4de7ceb"), "other")
            ))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/{podcastId}/items/{id}/playlists", item.podcast.id, item.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                          "content": [
                              {"id":"50958264-d5ed-4a9a-a875-5173bb207720","name":"foo"}, 
                              {"id":"e053b63c-dc1d-4a3a-9c95-8f616a74d2aa","name":"bar"}, 
                              {"id":"6761208b-85e7-4098-817a-2db7c4de7ceb","name":"other"}
                          ]
                        }""")
                    }
        }
    }

    @Nested
    @DisplayName("should delete item")
    inner class ShouldDeleteItem {

        @Test
        fun `by id`() {
            /* Given */
            val id = UUID.randomUUID()
            whenever(itemService.deleteById(id)).thenReturn(Mono.empty())
            /* When */
            rest
                    .delete()
                    .uri("/api/v1/podcasts/5957cb8a-a05c-4ac3-bb03-94f473d38273/items/{id}", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
        }
    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun fixedClock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
    }
}

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)
