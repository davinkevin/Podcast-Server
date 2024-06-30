package com.github.davinkevin.podcastserver.update.updaters

import com.github.davinkevin.podcastserver.service.image.CoverInformation
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import kotlin.time.measureTimedValue

val log = LoggerFactory.getLogger(Updater::class.java)!!

interface Updater {

    fun update(podcast: PodcastToUpdate): UpdatePodcastInformation? {
        log.info("podcast {} starts update", podcast.url)
        val (value, duration) = measureTimedValue {
            val signature = runCatching { signatureOf(podcast.url).block() }
                .onFailure { log.error("podcast {} ends with error", podcast.url, it) }
                .getOrNull()
                ?: return@measureTimedValue null

            if (podcast.signature == signature) {
                log.debug("podcast {} hasn't change", podcast.url)
                return@measureTimedValue null
            }

            log.debug("podcast {} has new signature {}", podcast.url, signature)

            val items = runCatching { findItems(podcast).collectList().block()!!.toSet() }
                .onFailure { log.error("podcast {} ends with error", podcast.url, it) }
                .getOrNull() ?: return@measureTimedValue null

            if (items.isEmpty())
                log.warn("podcast {} has no item found, potentially updater {} not working anymore", podcast.url, this::class)
            else
                log.debug("podcast {} has {} items found", podcast.url, items.size)

            return@measureTimedValue UpdatePodcastInformation(podcast, items, signature)
        }

        log.info("podcast {} ends update after {} seconds", podcast.url, duration.inWholeSeconds)

        return value
    }

    fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> = Mono.fromCallable { findItemsBlocking(podcast) }.flatMapIterable { it }
    fun signatureOf(url: URI): Mono<String> = Mono.fromCallable { signatureOfBlocking(url) }

    fun findItemsBlocking(podcast: PodcastToUpdate): List<ItemFromUpdate> = emptyList()
    fun signatureOfBlocking(url: URI): String? = ""

    fun type(): Type
    fun compatibility(url: String): Int
}


data class UpdatePodcastInformation(val podcast: PodcastToUpdate, val items: Set<ItemFromUpdate>, val newSignature: String)
data class PodcastToUpdate(val id: UUID, val url: URI, val signature: String)
data class ItemFromUpdate(
        val title: String?,
        val pubDate: ZonedDateTime?,
        val length: Long? = null,
        val mimeType: String,
        val url: URI,
        val guid: String? = null,
        val description: String?,
        val cover: Cover?,
) {

    fun guidOrUrl(): String = guid ?: url.toASCIIString()

    data class Cover(val width: Int, val height: Int, val url: URI)
}

fun CoverInformation.toCoverFromUpdate() = ItemFromUpdate.Cover(
        height = this@toCoverFromUpdate.height,
        width = this@toCoverFromUpdate.width,
        url = this@toCoverFromUpdate.url
)