package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Created by kevin on 2018-11-25
 */
class MessagingTemplate {
    val messages: Sinks.Many<Message<out Any>> = Sinks.many().multicast().directBestEffort()

    fun sendWaitingQueue(value: List<DownloadingItem>) = messages.tryEmitNext(WaitingQueueMessage(value))
    fun sendItem(value: DownloadingItem) = messages.tryEmitNext(DownloadingItemMessage(value))
    fun isUpdating(value: Boolean) = messages.tryEmitNext(UpdateMessage(value))

    fun messagesAsFlux(): Flux<Message<out Any>> = messages.asFlux()
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: List<DownloadingItem>): Message<List<DownloadingItem>>("waiting", value)
class DownloadingItemMessage(value: DownloadingItem): Message<DownloadingItem>("downloading", value)
