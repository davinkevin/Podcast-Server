package com.github.davinkevin.podcastserver.controller.sse

import com.github.davinkevin.podcastserver.service.Message
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter


/**
 * Created by kevin on 2018-11-25
 */
@Controller
@RequestMapping("/api/sse")
class DownloaderControllerSSE {

    val log = LoggerFactory.getLogger(this.javaClass.name)!!

    private var emitters: Set<SseEmitter> = setOf()

    @GetMapping
    fun sseMessages(): ResponseEntity<SseEmitter> {
        val emitter = SseEmitter(Long.MAX_VALUE)
        emitters += emitter

        emitter.onCompletion { emitters -= emitter }
        emitter.onTimeout { emitters -= emitter }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noTransform())
                .body(emitter)
    }

    @EventListener fun <T> listener(v: Message<T>) = send(v)

    private fun <T> send(v: Message<T>) {
        val event = SseEmitter.event().data(v.value as Any).name(v.topic)
        val deadEmitters = mutableListOf<SseEmitter>()
        emitters.forEach {
            try { it.send(event) }
            catch (e: Exception) { deadEmitters.add(it) }
        }
        emitters -= deadEmitters
    }
}