package com.github.davinkevin.podcastserver.controller.sse

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.service.DownloadingItemMessage
import com.github.davinkevin.podcastserver.service.Message
import com.github.davinkevin.podcastserver.service.UpdateMessage
import com.github.davinkevin.podcastserver.service.WaitingQueueMessage
import org.springframework.context.event.EventListener
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration
import java.time.Duration.*
import java.util.*


/**
 * Created by kevin on 2018-11-25
 */
@RestController
@RequestMapping("/api/sse")
class DownloaderControllerSSE {
    
    val messages = DirectProcessor.create<ServerSentEvent<Any>>()

    @GetMapping
    fun sseMessages() = Flux
            .interval(ofSeconds(5))
            .map { ServerSentEvent.builder(it as Any).event("heartbeat").build() }
            .mergeWith(messages.share())

    @EventListener
    fun <T> listener(v: Message<T>) = send(v)

    private fun <T> send(v: Message<T>) {
        val body: Any = when(v) {
            is DownloadingItemMessage -> toDownloadingItemHAL(v.value)
            is WaitingQueueMessage -> v.value.map { toDownloadingItemHAL(it) }
            is UpdateMessage -> v.value
        }

        val event = ServerSentEvent
                .builder(body)
                .event(v.topic)
                .build()

        messages.onNext(event)
    }
}

data class DownloadingItemHAL(
        val id: UUID,
        val title: String,
        val status: Status,
        val url: URI,
        val progression: Int,
        val podcast: Podcast,
        val cover: Cover
) {

    @JsonProperty("isDownloaded")
    private val isDownloaded = Status.FINISH == status

    data class Podcast(val id: UUID, val title: String)
    data class Cover(val id: UUID, val url: URI)
}

private fun toDownloadingItemHAL(item: DownloadingItem) = DownloadingItemHAL(
        id = item.id,
        title = item.title,
        status = item.status,
        url = item.url,
        progression = item.progression,
        podcast = DownloadingItemHAL.Podcast(item.podcast.id, item.podcast.title),
        cover = DownloadingItemHAL.Cover(item.cover.id, item.cover.url)
)
