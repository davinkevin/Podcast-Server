package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Sinks

/**
 * Created by kevin on 2018-11-25
 */
class MessagingTemplate {
    val messages = MutableSharedFlow<Message<out Any>>()

    fun sendWaitingQueue(value: List<DownloadingItem>) = messages.tryEmit(WaitingQueueMessage(value)).also { println("inside fun") }
    fun sendItem(value: DownloadingItem) = messages.tryEmit(DownloadingItemMessage(value))
    fun isUpdating(value: Boolean) = messages.tryEmit(UpdateMessage(value))
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: List<DownloadingItem>): Message<List<DownloadingItem>>("waiting", value)
class DownloadingItemMessage(value: DownloadingItem): Message<DownloadingItem>("downloading", value)
