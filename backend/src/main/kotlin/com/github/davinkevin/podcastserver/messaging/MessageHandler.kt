package com.github.davinkevin.podcastserver.messaging

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.Duration.ofSeconds
import java.util.*

class MessageHandler(
        private val mt: MessagingTemplate,
        private val mapper: ObjectMapper
) {

    private val messageToForward: Sinks.Many<Any> = Sinks.many().multicast().directBestEffort()

    fun sseMessages(@Suppress("UNUSED_PARAMETER") s: ServerRequest): Mono<ServerResponse> {

        val heartBeat = Flux.interval(ofSeconds(5))
                .mergeWith(0L.toMono())
                .map { ServerSentEvent.builder(it as Any)
                        .event("heartbeat")
                        .build()
                }

        val messages = mt.messages.asFlux().map { toSSE(it) }.share()
        val toForward = messageToForward.asFlux().share()

        return ok()
                .sse()
                .body(Flux.merge(messages, toForward, heartBeat))
    }
}

private fun <T> toSSE(v: Message<T>): ServerSentEvent<out Any> {
    val body: Any = when(v) {
        is UpdateMessage -> v.value
        is WaitingQueueMessage -> v.value.map(::toDownloadingItemHAL)
        is DownloadingItemMessage -> toDownloadingItemHAL(v.value)
    }

    return toServerSentEvent(v.topic, body)
}

private fun <T> toServerSentEvent(event: String, body: T): ServerSentEvent<T> {
    return ServerSentEvent
            .builder(body!!)
            .event(event)
            .build()
}

internal data class DownloadingItemHAL(
        val id: UUID,
        val title: String,
        val status: Status,
        val url: URI,
        val progression: Int,
        val podcast: Podcast,
        val cover: Cover
) {

    @JsonProperty("isDownloaded")
    val isDownloaded = Status.FINISH == status

    data class Podcast(val id: UUID, val title: String)
    data class Cover(val id: UUID, val url: URI)
}

internal fun toDownloadingItemHAL(item: DownloadingItem) = DownloadingItemHAL(
        id = item.id,
        title = item.title,
        status = item.status,
        url = item.url,
        progression = item.progression,
        podcast = DownloadingItemHAL.Podcast(item.podcast.id, item.podcast.title),
        cover = DownloadingItemHAL.Cover(item.cover.id, item.cover.url)
)
