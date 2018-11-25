package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.entity.Item
import io.vavr.collection.Queue
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Created by kevin on 2018-11-25
 */
@Service
class MessagingTemplate(val publisher: ApplicationEventPublisher) {

    @Suppress("UNCHECKED_CAST")
    fun <T> convertAndSend(s: String, value: T) {
        when {
            s == "/topic/updating" && value is Boolean -> publisher.publishEvent(UpdateMessage(value))
            s == "/topic/waiting" && value is Queue<*> -> publisher.publishEvent(WaitingQueueMessage(value as Queue<Item>))
            s == "/topic/download" && value is Item -> publisher.publishEvent(DownloadingItemMessage(value))
        }
    }
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: Queue<Item>): Message<Queue<Item>>("waiting", value)
class DownloadingItemMessage(value: Item): Message<Item>("downloading", value)