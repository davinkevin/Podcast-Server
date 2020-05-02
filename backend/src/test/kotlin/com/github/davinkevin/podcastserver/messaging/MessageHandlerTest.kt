package com.github.davinkevin.podcastserver.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import reactor.core.publisher.DirectProcessor
import reactor.test.StepVerifier
import java.net.URI
import java.util.*

/**
 * Created by kevin on 02/05/2020
 */
@WebFluxTest(controllers = [MessageHandler::class])
@Import(MessagingConfig::class)
@AutoConfigureWebTestClient(timeout = "PT15S")
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class MessageHandlerTest(
        @Autowired val rest: WebTestClient,
        @Autowired val mapper: ObjectMapper
) {

    @MockBean
    private lateinit var messageTemplate: MessagingTemplate

    @Nested
    @DisplayName("on syncing information")
    inner class OnSyncingInformation {

        private val item1 = DownloadingItemHAL(
                id = UUID.randomUUID(),
                title = "Title 1",
                status = Status.NOT_DOWNLOADED,
                url = URI("https://foo.bar.com/podcast/title-1"),
                progression = 0,
                podcast = DownloadingItemHAL.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItemHAL.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-1.jpg"))
        )
        private val item2 = DownloadingItemHAL(
                id = UUID.randomUUID(),
                title = "Title 2",
                status = Status.STARTED,
                url = URI("https://foo.bar.com/podcast/title-2"),
                progression = 50,
                podcast = DownloadingItemHAL.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItemHAL.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-2.jpg"))
        )
        private val item3 = DownloadingItemHAL(
                id = UUID.randomUUID(),
                title = "Title 3",
                status = Status.STARTED,
                url = URI("https://foo.bar.com/podcast/title-3"),
                progression = 75,
                podcast = DownloadingItemHAL.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItemHAL.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-3.jpg"))
        )
        private val item4 = DownloadingItemHAL(
                id = UUID.randomUUID(),
                title = "Title 4",
                status = Status.FINISH,
                url = URI("https://foo.bar.com/podcast/title-4"),
                progression = 100,
                podcast = DownloadingItemHAL.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItemHAL.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-4.jpg"))
        )

        private fun <T> sync(m: SyncMessage<T>) {
            rest
                    .post()
                    .uri("/api/v1/sse/sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(m)
                    .exchange()
                    .expectStatus()
                    .isOk
        }

        @BeforeEach
        fun beforeEach() {
            val messages = DirectProcessor.create<Message<out Any>>()
            whenever(messageTemplate.messages).thenReturn(messages)
        }

        @Nested
        @DisplayName("on update")
        inner class OnUpdate {

            @Test
            fun `should forward message to sse`() {
                /* Given */
                StepVerifier.create(rest
                        .get()
                        .uri("/api/v1/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .returnResult<ServerSentEvent<String>>()
                        .responseBody
                        .filter { it.event() != "heartbeat" }
                        .take(2)
                )
                        /* Then */
                        .expectSubscription()
                        .then {
                            /* When */
                            sync(SyncUpdateMessage(true))
                            sync(SyncUpdateMessage(false))
                        }
                        /* Then */
                        .assertNext {
                            assertThat(it.event()).isEqualTo("updating")
                            assertThat(it.data()).isEqualTo("true")
                        }
                        .assertNext {
                            assertThat(it.event()).isEqualTo("updating")
                            assertThat(it.data()).isEqualTo("false")
                        }
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("on downloading item")
        inner class OnDownloadingItem {

            @Test
            fun `should forward message to sse`() {
                /* Given */
                StepVerifier.create(rest
                        .get()
                        .uri("/api/v1/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .returnResult<ServerSentEvent<String>>()
                        .responseBody
                        .filter { it.event() != "heartbeat" }
                        .map { it.event() to mapper.readValue<DownloadingItemHAL>(it.data()!!) }
                        .take(2)
                )
                        .expectSubscription()
                        /* When */
                        .then {
                            sync(SyncDownloadingItemMessage(item1))
                            sync(SyncDownloadingItemMessage(item4))
                        }
                        /* Then */
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("downloading")
                            assertThat(body.id).isEqualTo(item1.id)
                            assertThat(body.title).isEqualTo(item1.title)
                            assertThat(body.status).isEqualTo(item1.status)
                            assertThat(body.url).isEqualTo(item1.url)
                            assertThat(body.progression).isEqualTo(item1.progression)
                            assertThat(body.podcast.id).isEqualTo(item1.podcast.id)
                            assertThat(body.podcast.title).isEqualTo(item1.podcast.title)
                            assertThat(body.cover.id).isEqualTo(item1.cover.id)
                            assertThat(body.cover.url).isEqualTo(item1.cover.url)
                            assertThat(body.isDownloaded).isEqualTo(false)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("downloading")
                            assertThat(body.id).isEqualTo(item4.id)
                            assertThat(body.title).isEqualTo(item4.title)
                            assertThat(body.status).isEqualTo(item4.status)
                            assertThat(body.url).isEqualTo(item4.url)
                            assertThat(body.progression).isEqualTo(item4.progression)
                            assertThat(body.podcast.id).isEqualTo(item4.podcast.id)
                            assertThat(body.podcast.title).isEqualTo(item4.podcast.title)
                            assertThat(body.cover.id).isEqualTo(item4.cover.id)
                            assertThat(body.cover.url).isEqualTo(item4.cover.url)
                            assertThat(body.isDownloaded).isEqualTo(true)
                        }
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("on waiting list change")
        inner class OnWaitingListChange {

            @Test
            fun `should forward message to sse`() {
                /* Given */
                StepVerifier.create(rest
                        .get()
                        .uri("/api/v1/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .returnResult<ServerSentEvent<String>>()
                        .responseBody
                        .filter { it.event() != "heartbeat" }
                        .map { it.event() to mapper.readValue<List<DownloadingItemHAL>>(it.data()!!) }
                        .take(4)
                )
                        /* When */
                        .expectSubscription()
                        .then {
                            sync(SyncWaitingQueueMessage(listOf(item1, item2, item3)))
                            sync(SyncWaitingQueueMessage(listOf(item2, item3)))
                            sync(SyncWaitingQueueMessage(listOf(item3)))
                            sync(SyncWaitingQueueMessage(emptyList()))
                        }
                        /* Then */
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(3)
                            assertThat(body[0].id).isEqualTo(item1.id)
                            assertThat(body[1].id).isEqualTo(item2.id)
                            assertThat(body[2].id).isEqualTo(item3.id)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(2)
                            assertThat(body[0].id).isEqualTo(item2.id)
                            assertThat(body[1].id).isEqualTo(item3.id)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(1)
                            assertThat(body[0].id).isEqualTo(item3.id)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(0)
                        }
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("with undefined type of message")
        inner class WithUndefinedTypeOfMessage {

            @Test
            fun `should return error`() {
                /* Given */
                /* When */
                rest
                        .post()
                        .uri("/api/v1/sse/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""{"event": "foo"}""")
                        .exchange()
                        /* Then */
                        .expectStatus()
                        .is5xxServerError
                        .expectBody()
                        .assertThatJson {
                            inPath("status").isEqualTo(500)
                            inPath("error").isEqualTo("Internal Server Error")
                            inPath("message").isEqualTo("message with event foo not supported")
                        }
            }
        }

    }

    @Nested
    @DisplayName("on streaming to client")
    inner class OnStreamingToClient {

        private val item1 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Title 1",
                status = Status.NOT_DOWNLOADED,
                url = URI("https://foo.bar.com/podcast/title-1"),
                numberOfFail = 0,
                progression = 0,
                podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-1.jpg"))
        )
        private val item2 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Title 2",
                status = Status.STARTED,
                url = URI("https://foo.bar.com/podcast/title-2"),
                numberOfFail = 0,
                progression = 50,
                podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-2.jpg"))
        )
        private val item3 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Title 3",
                status = Status.STARTED,
                url = URI("https://foo.bar.com/podcast/title-3"),
                numberOfFail = 3,
                progression = 75,
                podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-3.jpg"))
        )
        private val item4 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Title 4",
                status = Status.FINISH,
                url = URI("https://foo.bar.com/podcast/title-4"),
                numberOfFail = 0,
                progression = 100,
                podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
                cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-4.jpg"))
        )

        @Nested
        @DisplayName("on downloading item")
        inner class OnDownloadingItem {

            @Test
            fun `should receive items`() {
                /* Given */
                val messages = DirectProcessor.create<Message<out Any>>()
                whenever(messageTemplate.messages).thenReturn(messages)

                /* When */
                StepVerifier.create(rest
                        .get()
                        .uri("/api/v1/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .returnResult<ServerSentEvent<String>>()
                        .responseBody
                        .filter { it.event() != "heartbeat" }
                        .map { it.event() to mapper.readValue<DownloadingItemHAL>(it.data()!!) }
                        .take(2)
                )
                        /* Then */
                        .expectSubscription()
                        .then {
                            messages.onNext(DownloadingItemMessage(item1))
                            messages.onNext(DownloadingItemMessage(item4))
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("downloading")
                            assertThat(body.id).isEqualTo(item1.id)
                            assertThat(body.title).isEqualTo(item1.title)
                            assertThat(body.status).isEqualTo(item1.status)
                            assertThat(body.url).isEqualTo(item1.url)
                            assertThat(body.progression).isEqualTo(item1.progression)
                            assertThat(body.podcast.id).isEqualTo(item1.podcast.id)
                            assertThat(body.podcast.title).isEqualTo(item1.podcast.title)
                            assertThat(body.cover.id).isEqualTo(item1.cover.id)
                            assertThat(body.cover.url).isEqualTo(item1.cover.url)
                            assertThat(body.isDownloaded).isEqualTo(false)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("downloading")
                            assertThat(body.id).isEqualTo(item4.id)
                            assertThat(body.title).isEqualTo(item4.title)
                            assertThat(body.status).isEqualTo(item4.status)
                            assertThat(body.url).isEqualTo(item4.url)
                            assertThat(body.progression).isEqualTo(item4.progression)
                            assertThat(body.podcast.id).isEqualTo(item4.podcast.id)
                            assertThat(body.podcast.title).isEqualTo(item4.podcast.title)
                            assertThat(body.cover.id).isEqualTo(item4.cover.id)
                            assertThat(body.cover.url).isEqualTo(item4.cover.url)
                            assertThat(body.isDownloaded).isEqualTo(true)
                        }
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("on update")
        inner class OnUpdate {

            @Test
            fun `should receive updates`() {
                /* Given */
                val messages = DirectProcessor.create<Message<out Any>>()
                whenever(messageTemplate.messages).thenReturn(messages)

                /* When */
                StepVerifier.create(rest
                        .get()
                        .uri("/api/v1/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .returnResult<ServerSentEvent<String>>()
                        .responseBody
                        .filter { it.event() != "heartbeat" }
                        .take(2)
                )
                        /* Then */
                        .expectSubscription()
                        .then {
                            messages.onNext(UpdateMessage(true))
                            messages.onNext(UpdateMessage(false))
                        }
                        .assertNext {
                            assertThat(it.event()).isEqualTo("updating")
                            assertThat(it.data()).isEqualTo("true")
                        }
                        .assertNext {
                            assertThat(it.event()).isEqualTo("updating")
                            assertThat(it.data()).isEqualTo("false")
                        }
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("on waiting list change")
        inner class OnWaitingListChange {

            @Test
            fun `should receive new waiting list`() {
                /* Given */
                val messages = DirectProcessor.create<Message<out Any>>()
                whenever(messageTemplate.messages).thenReturn(messages)

                /* When */
                StepVerifier.create(rest
                        .get()
                        .uri("/api/v1/sse")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .exchange()
                        .expectStatus()
                        .isOk
                        .returnResult<ServerSentEvent<String>>()
                        .responseBody
                        .filter { it.event() != "heartbeat" }
                        .map { it.event() to mapper.readValue<List<DownloadingItemHAL>>(it.data()!!) }
                        .take(4)
                )
                        /* Then */
                        .expectSubscription()
                        .then {
                            messages.onNext(WaitingQueueMessage(listOf(item1, item2, item3)))
                            messages.onNext(WaitingQueueMessage(listOf(item2, item3)))
                            messages.onNext(WaitingQueueMessage(listOf(item3)))
                            messages.onNext(WaitingQueueMessage(emptyList()))
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(3)
                            assertThat(body[0].id).isEqualTo(item1.id)
                            assertThat(body[1].id).isEqualTo(item2.id)
                            assertThat(body[2].id).isEqualTo(item3.id)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(2)
                            assertThat(body[0].id).isEqualTo(item2.id)
                            assertThat(body[1].id).isEqualTo(item3.id)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(1)
                            assertThat(body[0].id).isEqualTo(item3.id)
                        }
                        .assertNext { (event, body) ->
                            assertThat(event).isEqualTo("waiting")
                            assertThat(body).hasSize(0)
                        }
                        .verifyComplete()
            }

        }

        @Test
        fun `should receive heartbeat`() {
            /* Given */
            whenever(messageTemplate.messages)
                    .thenReturn(DirectProcessor.create())

            /* When */
            StepVerifier.create(rest
                    .get()
                    .uri("/api/v1/sse")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .returnResult<ServerSentEvent<Int>>()
                    .responseBody
                    .take(2)
            )
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.event()).isEqualTo("heartbeat")
                    }
                    .assertNext {
                        assertThat(it.event()).isEqualTo("heartbeat")
                    }
                    .verifyComplete()
        }
    }

}
