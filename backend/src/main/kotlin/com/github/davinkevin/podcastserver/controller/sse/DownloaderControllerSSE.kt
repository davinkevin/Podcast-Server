package com.github.davinkevin.podcastserver.controller.sse

import com.github.davinkevin.podcastserver.service.Message
import org.springframework.context.event.EventListener
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.DirectProcessor


/**
 * Created by kevin on 2018-11-25
 */
@RestController
@RequestMapping("/api/sse")
class DownloaderControllerSSE {
    
    val messages = DirectProcessor.create<ServerSentEvent<Any>>()

    @GetMapping
    fun sseMessages() = messages.share()

    @EventListener
    fun <T> listener(v: Message<T>) = send(v)

    private fun <T> send(v: Message<T>) {
        val event = ServerSentEvent
                .builder(v.value as Any)
                .event(v.topic)
                .build()

        messages.onNext(event)
    }
}