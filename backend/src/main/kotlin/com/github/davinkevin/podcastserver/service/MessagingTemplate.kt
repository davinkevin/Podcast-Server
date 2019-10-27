package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.springframework.context.ApplicationEventPublisher

/**
 * Created by kevin on 2018-11-25
 */
class MessagingTemplate(val publisher: ApplicationEventPublisher) {

    fun sendWaitingQueue(value: List<DownloadingItem>) {
        publisher.publishEvent(WaitingQueueMessage(value))
    }

    fun sendItem(value: DownloadingItem) {
        publisher.publishEvent(DownloadingItemMessage(value))
    }

    fun isUpdating(value: Boolean) {
        publisher.publishEvent(UpdateMessage(value))
    }
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: List<DownloadingItem>): Message<List<DownloadingItem>>("waiting", value)
class DownloadingItemMessage(value: DownloadingItem): Message<DownloadingItem>("downloading", value)
