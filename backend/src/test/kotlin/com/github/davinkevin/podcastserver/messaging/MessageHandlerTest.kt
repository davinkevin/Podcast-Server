package com.github.davinkevin.podcastserver.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.servlet.function.ServerResponse.SseBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@WebMvcTest(controllers = [MessageHandler::class])
@Import(MessagingRoutingConfig::class)
class MessageHandlerTest(
    @Autowired val rest: WebTestClient,
    @Autowired val mapper: ObjectMapper
) {

    @MockitoBean private lateinit var messageTemplate: MessagingTemplate

    @Nested
    @Disabled("Impossible to test infinite stream with fake Spring Servlet: https://github.com/spring-projects/spring-framework/issues/32687")
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
                val messages = Sinks.many().multicast().directBestEffort<Message<out Any>>()

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
                        messages.tryEmitNext(DownloadingItemMessage(item1))
                        messages.tryEmitNext(DownloadingItemMessage(item4))
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
                val messages = Sinks.many().multicast().directBestEffort<Message<out Any>>()

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
                        messages.tryEmitNext(UpdateMessage(true))
                        messages.tryEmitNext(UpdateMessage(false))
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
                val messages = Sinks.many().multicast().directBestEffort<Message<out Any>>()

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
                        messages.tryEmitNext(WaitingQueueMessage(listOf(item1, item2, item3)))
                        messages.tryEmitNext(WaitingQueueMessage(listOf(item2, item3)))
                        messages.tryEmitNext(WaitingQueueMessage(listOf(item3)))
                        messages.tryEmitNext(WaitingQueueMessage(emptyList()))
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
            /* When */
            StepVerifier.create(
                rest
                    .get()
                    .uri("/api/v1/sse")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    /* Then */
                    .exchange()
                    .expectStatus()
                    .isOk
                    .returnResult<ServerSentEvent<Int>>()
                    .responseBody
                    .take(2)
            )
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

    @Nested
    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
    @DisplayName("Using implementation details")
    inner class UsingHandlerImplementationDetails {

        @Suppress("ReactiveStreamsUnusedPublisher")
        fun sseBuilderToFlux(): Pair<SseBuilder, Flux<LocalSSEvent>> {
            val sse = mock<SseBuilder>()
            val events = Flux.generate { sink ->
                val currentEvent = LocalSSEvent()
                reset(sse)
                whenever(sse.event(anyString())).then {
                    currentEvent.event = it.arguments[0] as String
                    return@then sse
                }
                whenever(sse.send(any())).then {
                    currentEvent.data = it.arguments[0]
                    return@then sse
                }

                Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .until { currentEvent.event != null && currentEvent.data != null }

                sink.next(currentEvent)
            }

            return sse to events
        }

        @Suppress("UNCHECKED_CAST")
        fun startConsumer(m: MessagingTemplate, sse: SseBuilder) {
            val sseAnswer = MessageHandler().sseMessages(mock())
            val eventConsumer: Consumer<SseBuilder> = ReflectionTestUtils.getField(sseAnswer,"sseConsumer") as Consumer<SseBuilder>
            eventConsumer.accept(sse)
        }

        @Nested
        @DisplayName("on streaming to client")
        @Disabled("Too slow, covered by UsingPrivateFunction::OnStreamingToClient::*")
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
                    val (sse, events) = sseBuilderToFlux()
                    startConsumer(messageTemplate, sse)

                    /* When */
                    StepVerifier.create(
                        events
                            .filter { it.event != "heartbeat" }
                            .map { it.event to it.data as DownloadingItemHAL }
                            .take(2)
                    )
                        /* Then */
                        .expectSubscription()
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
                    val (sse, events) = sseBuilderToFlux()
                    startConsumer(messageTemplate, sse)

                    /* When */
                    StepVerifier.create(events
                        .filter { it.event != "heartbeat" }
                        .take(2)
                    )
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.event).isEqualTo("updating")
                            assertThat(it.data).isEqualTo(true)
                        }
                        .assertNext {
                            assertThat(it.event).isEqualTo("updating")
                            assertThat(it.data).isEqualTo(false)
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
                    val (sse, events) = sseBuilderToFlux()
                    startConsumer(messageTemplate, sse)

                    /* When */
                    StepVerifier.create(
                        events
                            .filter { it.event != "heartbeat" }
                            .map { it.event to it.data as List<DownloadingItemHAL> }
                            .take(4)
                    )
                        /* Then */
                        .expectSubscription()
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
        }

        @Test
        fun `should receive heartbeat`() {
            /* Given */
            val (sse, events) = sseBuilderToFlux()
            startConsumer(messageTemplate, sse)

            /* When */
            StepVerifier.create(events.take(1))
                .expectSubscription()
                .assertNext {
                    assertThat(it.event).isEqualTo("heartbeat")
                }
                .verifyComplete()
        }
    }

    @Nested
    @Suppress("UNCHECKED_CAST")
    @DisplayName("UsingPrivateFunction")
    inner class UsingPrivateFunction {

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
                    val handler = MessageHandler()
                    /* When */
                    StepVerifier.create(
                        handler.streamingMessages()
                            .filter { it.event != "heartbeat" }
                            .map { it.event to it.body as DownloadingItemHAL }
                            .take(2)
                    )
                        /* Then */
                        .expectSubscription()
                        .then {
                            listOf(DownloadingItemMessage(item1), DownloadingItemMessage(item4))
                                .forEach(handler::receive)
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
                    val handler = MessageHandler()
                    /* When */
                    StepVerifier.create(
                        handler.streamingMessages()
                            .filter { it.event != "heartbeat" }
                            .take(2)
                    )
                        /* Then */
                        .expectSubscription()
                        .then {
                            listOf(UpdateMessage(true), UpdateMessage(false))
                                .forEach(handler::receive)
                        }
                        .assertNext {
                            assertThat(it.event).isEqualTo("updating")
                            assertThat(it.body).isEqualTo(true)
                        }
                        .assertNext {
                            assertThat(it.event).isEqualTo("updating")
                            assertThat(it.body).isEqualTo(false)
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
                    val handler = MessageHandler()

                    /* When */
                    StepVerifier.create(
                        handler.streamingMessages()
                            .filter { it.event != "heartbeat" }
                            .map { it.event to it.body as List<DownloadingItemHAL> }
                            .take(4)
                    )
                        /* Then */
                        .expectSubscription()
                        .then {
                            listOf(
                                WaitingQueueMessage(listOf(item1, item2, item3)),
                                WaitingQueueMessage(listOf(item2, item3)),
                                WaitingQueueMessage(listOf(item3)),
                                WaitingQueueMessage(emptyList())
                            )
                                .forEach(handler::receive)
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
        }

        @Test
        fun `should receive heartbeat`() {
            /* Given */
            /* When */
            StepVerifier.withVirtualTime { MessageHandler().streamingMessages().take(2) }
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(1))
                .assertNext { assertThat(it.event).isEqualTo("heartbeat") }
                .thenAwait(Duration.ofSeconds(1))
                .assertNext { assertThat(it.event).isEqualTo("heartbeat") }
                .verifyComplete()
        }
    }
}

data class LocalSSEvent(var event: String? = null, var data: Any? = null)