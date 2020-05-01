package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import reactor.core.publisher.DirectProcessor

/**
 * Created by kevin on 2018-11-25
 */
class MessagingTemplate {
    val messages: DirectProcessor<Message<out Any>> = DirectProcessor.create()

    fun sendWaitingQueue(value: List<DownloadingItem>) = messages.onNext(WaitingQueueMessage(value))
    fun sendItem(value: DownloadingItem) = messages.onNext(DownloadingItemMessage(value))
    fun isUpdating(value: Boolean) = messages.onNext(UpdateMessage(value))
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: List<DownloadingItem>): Message<List<DownloadingItem>>("waiting", value)
class DownloadingItemMessage(value: DownloadingItem): Message<DownloadingItem>("downloading", value)
