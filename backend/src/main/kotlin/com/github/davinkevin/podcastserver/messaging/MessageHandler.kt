package com.github.davinkevin.podcastserver.messaging

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration.ZERO
import java.time.Duration.ofSeconds
import java.util.*

@ExperimentalCoroutinesApi
class MessageHandler(private val mt: MessagingTemplate) {

    suspend fun sseMessages(@Suppress("UNUSED_PARAMETER") s: ServerRequest): ServerResponse {

        val heartBeat = Flux.interval(ZERO, ofSeconds(5))
            .map { toHeartBeat(it) }
            .asFlow()

        val messages = mt.messages
            .map { it.toSSE() }

        return ok()
            .sse()
            .bodyAndAwait(merge(heartBeat, messages))
    }
}

private fun <T> Message<T>.toSSE(): ServerSentEvent<out Any> {
    val body: Any = when(this) {
        is UpdateMessage -> value
        is WaitingQueueMessage -> value.map(DownloadingItem::toDownloadingItemHAL)
        is DownloadingItemMessage -> value.toDownloadingItemHAL()
    }

    return toServerSentEvent(topic, body)
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

internal fun DownloadingItem.toDownloadingItemHAL() = DownloadingItemHAL(
    id = id,
    title = title,
    status = status,
    url = url,
    progression = progression,
    podcast = DownloadingItemHAL.Podcast(podcast.id, podcast.title),
    cover = DownloadingItemHAL.Cover(cover.id, cover.url)
)
