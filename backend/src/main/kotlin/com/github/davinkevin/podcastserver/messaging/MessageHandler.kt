package com.github.davinkevin.podcastserver.messaging

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.sse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration.ZERO
import java.time.Duration.ofSeconds
import java.util.*

class MessageHandler(private val mt: MessagingTemplate) {

    fun sseMessages(@Suppress("UNUSED_PARAMETER") s: ServerRequest): Mono<ServerResponse> {

        val heartBeat = Flux.interval(ZERO, ofSeconds(5))
                .map { toHeartBeat(it) }

        val messages = mt.messages.asFlux()
                .map { toSSE(it) }
                .share()

        return ok()
                .sse()
                .body(Flux.merge(messages, heartBeat))
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

private fun toHeartBeat(v: Long): ServerSentEvent<out Any> = ServerSentEvent
        .builder(v)
        .event("heartbeat")
        .build()

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
