package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.util.*

@WebMvcTest(controllers = [DownloadHandler::class])
@Import(DownloadRouterConfig::class)
@ImportAutoConfiguration(ErrorMvcAutoConfiguration::class)
class DownloadHandlerTest(
        @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var idm: ItemDownloadManager

    private val item1 = DownloadingItem(
            id = UUID.fromString("6c05149f-a3e1-4302-ab1f-83324c75ad70"),
            url = URI("https://foo.bar.com/1"),
            title = "item1",
            status = Status.PAUSED,
            progression = 0,
            numberOfFail = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.fromString("acaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                    title = "podcast"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.fromString("cc05149f-a3e1-4302-ab1f-83324c75ad70"),
                    url = URI("https://foo.bar.com/item1/url.png")
            )
    )

    private val item2 = DownloadingItem(
            id = UUID.fromString("7caba8f2-4f2f-49f0-a520-a48bc628d81f"),
            url = URI("https://foo.bar.com/2"),
            title = "item2",
            status = Status.STARTED,
            progression = 10,
            numberOfFail = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.fromString("acaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                    title = "podcast"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.fromString("ccaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                    url = URI("https://foo.bar.com/item2/url.png")
            )
    )

    @Nested
    @DisplayName("should download item")
    inner class ShouldDownloadItem {

        @Test
        fun `with success`() {
            /* Given */
            val id = UUID.randomUUID()
            doNothing().whenever(idm).addItemToQueue(id)
            /* When */
            rest
                    .post()
                    .uri("/api/v1/podcasts/foo/items/{id}/download", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent

            verify(idm).addItemToQueue(id)
        }

    }

    @Nested
    @DisplayName("should provide downloading list")
    inner class ShouldProvideDownloadingList {

        @Test
        fun `with success`() {
            /* Given */
            whenever(idm.downloading).thenReturn(listOf(item1, item2))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/downloads/downloading")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                           "items":[
                              {
                                 "id":"6c05149f-a3e1-4302-ab1f-83324c75ad70",
                                 "title":"item1",
                                 "status":"PAUSED",
                                 "progression": 0,
                                 "podcast":{
                                    "id":"acaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "title":"podcast"
                                 },
                                 "cover":{
                                    "id":"cc05149f-a3e1-4302-ab1f-83324c75ad70",
                                    "url":"/api/v1/podcasts/acaba8f2-4f2f-49f0-a520-a48bc628d81f/items/6c05149f-a3e1-4302-ab1f-83324c75ad70/cover.png"
                                 }
                              },
                              {
                                 "id":"7caba8f2-4f2f-49f0-a520-a48bc628d81f",
                                 "title":"item2",
                                 "status":"STARTED",
                                 "progression": 10,
                                 "podcast":{
                                    "id":"acaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "title":"podcast"
                                 },
                                 "cover":{
                                    "id":"ccaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "url":"/api/v1/podcasts/acaba8f2-4f2f-49f0-a520-a48bc628d81f/items/7caba8f2-4f2f-49f0-a520-a48bc628d81f/cover.png"
                                 }
                              }
                           ]
                        }""")
                    }
        }

        @Test
        fun `with cover with extension containing parameters`() {
            /* Given */
            val dlItem = DownloadingItem(
                    id = UUID.fromString("7caba8f2-4f2f-49f0-a520-a48bc628d81f"),
                    url = URI("https://foo.bar.com/2"),
                    title = "item2",
                    status = Status.STARTED,
                    progression = 10,
                    numberOfFail = 0,
                    podcast = DownloadingItem.Podcast(
                            id = UUID.fromString("acaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                            title = "podcast"
                    ),
                    cover = DownloadingItem.Cover(
                            id = UUID.fromString("ccaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                            url = URI("https://foo.bar.com/item2/url")
                    )
            )

            whenever(idm.downloading).thenReturn(listOf(dlItem))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/downloads/downloading")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                           "items":[
                              {
                                 "id":"7caba8f2-4f2f-49f0-a520-a48bc628d81f",
                                 "title":"item2",
                                 "status":"STARTED",
                                 "progression": 10,
                                 "podcast":{
                                    "id":"acaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "title":"podcast"
                                 },
                                 "cover":{
                                    "id":"ccaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "url":"/api/v1/podcasts/acaba8f2-4f2f-49f0-a520-a48bc628d81f/items/7caba8f2-4f2f-49f0-a520-a48bc628d81f/cover.jpg"
                                 }
                              }
                           ]
                        }""")
                    }
        }

        @Test
        fun `with cover without extension`() {
            /* Given */
            val dlItem = DownloadingItem(
                    id = UUID.fromString("7caba8f2-4f2f-49f0-a520-a48bc628d81f"),
                    url = URI("https://foo.bar.com/2"),
                    title = "item2",
                    status = Status.STARTED,
                    progression = 10,
                    numberOfFail = 0,
                    podcast = DownloadingItem.Podcast(
                            id = UUID.fromString("acaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                            title = "podcast"
                    ),
                    cover = DownloadingItem.Cover(
                            id = UUID.fromString("ccaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                            url = URI("https://foo.bar.com/item2/url.png?foo=bar")
                    )
            )

            whenever(idm.downloading).thenReturn(listOf(dlItem))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/downloads/downloading")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                           "items":[
                              {
                                 "id":"7caba8f2-4f2f-49f0-a520-a48bc628d81f",
                                 "title":"item2",
                                 "status":"STARTED",
                                 "progression": 10,
                                 "podcast":{
                                    "id":"acaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "title":"podcast"
                                 },
                                 "cover":{
                                    "id":"ccaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "url":"/api/v1/podcasts/acaba8f2-4f2f-49f0-a520-a48bc628d81f/items/7caba8f2-4f2f-49f0-a520-a48bc628d81f/cover.png"
                                 }
                              }
                           ]
                        }""")
                    }
        }


    }

    @Nested
    @DisplayName("should find limit number of parallel downloads")
    inner class ShouldFindLimitNumberOfParallelDownloads {

        @Test
        fun `with success`() {
            /* Given */
            whenever(idm.limitParallelDownload).thenReturn(12)
            /* When */
            rest
                    .get()
                    .uri("/api/v1/downloads/limit")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .consumeWith {
                        val limit = String(it.responseBody!!).toInt()
                        assertThat(limit).isEqualTo(12)
                    }
        }

    }

    @Nested
    @DisplayName("should update limit number of parallel downloads")
    inner class ShouldUpdateLimitNumberOfParallelDownloads {

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            rest
                    .post()
                    .uri("/api/v1/downloads/limit")
                    .bodyValue(12)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .consumeWith {
                        val limit = String(it.responseBody!!).toInt()
                        assertThat(limit).isEqualTo(12)
                    }

            verify(idm).limitParallelDownload = 12
        }
    }

    @Nested
    @DisplayName("should stop all downloads")
    inner class ShouldStopAllDownloads {

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            rest
                    .post()
                    .uri("/api/v1/downloads/stop")
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

            verify(idm).stopAllDownload()
        }
    }


    @Nested
    @DisplayName("should stop one download")
    inner class ShouldStopOneDownload {

        @Test
        fun `with success`() {
            /* Given */
            val id = UUID.randomUUID()
            doNothing().whenever(idm).removeItemFromQueueAndDownload(id)
            /* When */
            rest
                    .post()
                    .uri("/api/v1/downloads/{id}/stop", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

            verify(idm).removeItemFromQueueAndDownload(id)
        }
    }

    @Nested
    @DisplayName("should provide waiting queue")
    inner class ShouldProvideWaitingQueue {

        @Test
        fun `with success`() {
            /* Given */
            whenever(idm.queue).thenReturn(listOf(item1, item2))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/downloads/queue")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                           "items":[
                              {
                                 "id":"6c05149f-a3e1-4302-ab1f-83324c75ad70",
                                 "title":"item1",
                                 "status":"PAUSED",
                                 "progression": 0,
                                 "podcast":{
                                    "id":"acaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "title":"podcast"
                                 },
                                 "cover":{
                                    "id":"cc05149f-a3e1-4302-ab1f-83324c75ad70",
                                    "url":"/api/v1/podcasts/acaba8f2-4f2f-49f0-a520-a48bc628d81f/items/6c05149f-a3e1-4302-ab1f-83324c75ad70/cover.png"
                                 }
                              },
                              {
                                 "id":"7caba8f2-4f2f-49f0-a520-a48bc628d81f",
                                 "title":"item2",
                                 "status":"STARTED",
                                 "progression": 10,
                                 "podcast":{
                                    "id":"acaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "title":"podcast"
                                 },
                                 "cover":{
                                    "id":"ccaba8f2-4f2f-49f0-a520-a48bc628d81f",
                                    "url":"/api/v1/podcasts/acaba8f2-4f2f-49f0-a520-a48bc628d81f/items/7caba8f2-4f2f-49f0-a520-a48bc628d81f/cover.png"
                                 }
                              }
                           ]
                        }""")
                    }
        }
    }

    @Nested
    @DisplayName("should move in queue")
    inner class ShouldMoveInQueue {

        @Test
        fun `with success`() {
            /* Given */
            val operation = MovingItemInQueueForm(UUID.randomUUID(), 5)
            doNothing().whenever(idm).moveItemInQueue(operation.id, operation.position)
            /* When */
            rest
                    .post()
                    .uri("/api/v1/downloads/queue")
                    .bodyValue(operation)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
                    .expectBody().isEmpty
        }
    }

    @Nested
    @DisplayName("should remove from queue")
    inner class ShouldRemoveFromQueue {

        @Test
        fun `and totally stop the item`() {
            /* Given */
            val id = UUID.randomUUID()
            /* When */
            rest
                    .delete()
                    .uri("/api/v1/downloads/queue/{id}?stop=true", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

            verify(idm).removeItemFromQueue(id, true)
        }

        @Test
        fun `and not stop the item`() {
            /* Given */
            val id = UUID.randomUUID()
            /* When */
            rest
                    .delete()
                    .uri("/api/v1/downloads/queue/{id}?stop=false", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

            verify(idm).removeItemFromQueue(id, false)
        }

        @Test
        fun `and not stop the item by default`() {
            /* Given */
            val id = UUID.randomUUID()
            /* When */
            rest
                    .delete()
                    .uri("/api/v1/downloads/queue/{id}", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

            verify(idm).removeItemFromQueue(id, false)
        }

    }
}
