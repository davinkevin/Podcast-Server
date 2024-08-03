package com.github.davinkevin.podcastserver.messaging

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.google.common.annotations.VisibleForTesting
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration.ZERO
import java.time.Duration.ofSeconds
import java.util.*

class MessageHandler(private val mt: MessagingTemplate) {

    fun sseMessages(@Suppress("UNUSED_PARAMETER") s: ServerRequest): ServerResponse =
        ServerResponse.sse { sse ->
            streamingMessages()
                .subscribe {
                    sse.apply {
                        event(it.event)
                        runCatching { send(it.body) }
                    }
                }
        }

    @VisibleForTesting()
    internal fun streamingMessages(): Flux<ServerSentEvent<out Any>> {
        val heartBeat = Flux.interval(ZERO, ofSeconds(1))
            .map(::toHeartBeat)

        val messages = mt.messagesAsFlux()
            .map { toSSE(it) }
            .share()

        return Flux.merge(heartBeat, messages)
    }
}

private fun <T> toSSE(v: Message<T>): ServerSentEvent<out Any> {
    val body: Any = when (v) {
        is UpdateMessage -> v.value
        is WaitingQueueMessage -> v.value.map(::toDownloadingItemHAL)
        is DownloadingItemMessage -> toDownloadingItemHAL(v.value)
    }

    return ServerSentEvent(event = v.topic, body = body)
}

private fun toHeartBeat(v: Long): ServerSentEvent<out Any> =
    ServerSentEvent(event = "heartbeat", body = v)

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

internal data class ServerSentEvent<T>(val event: String, val body: T)