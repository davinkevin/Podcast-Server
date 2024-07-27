package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.springframework.context.ApplicationEventPublisher

/**
 * Created by kevin on 2018-11-25
 */
class MessagingTemplate(
    private val event: ApplicationEventPublisher
) {
    fun sendWaitingQueue(value: List<DownloadingItem>) {
        val v = WaitingQueueMessage(value)
        Thread.ofVirtual().start { event.publishEvent(v) }
    }
    fun sendItem(value: DownloadingItem) {
        val v = DownloadingItemMessage(value)
        Thread.ofVirtual().start { event.publishEvent(v) }
    }
    fun isUpdating(value: Boolean) {
        val v = UpdateMessage(value)
        Thread.ofVirtual().start { event.publishEvent(v) }
    }
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: List<DownloadingItem>): Message<List<DownloadingItem>>("waiting", value)
class DownloadingItemMessage(value: DownloadingItem): Message<DownloadingItem>("downloading", value)
