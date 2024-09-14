package com.github.davinkevin.podcastserver.update.updaters

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import kotlin.time.measureTimedValue

val log = LoggerFactory.getLogger(Updater::class.java)!!

interface Updater {

    val registry: MeterRegistry

    fun update(podcast: PodcastToUpdate): UpdatePodcastInformation? {
        log.info("podcast {} starts update", podcast.url)
        val (value, duration) = measureTimedValue {
            val signature = runCatching { signatureOf(podcast.url) }
                .onFailure { log.error("podcast {} ends with error", podcast.url, it) }
                .getOrNull()
                ?: return@measureTimedValue null

            if (podcast.signature == signature) {
                log.debug("podcast {} hasn't change", podcast.url)
                return@measureTimedValue null
            }

            log.debug("podcast {} has new signature {}", podcast.url, signature)

            val items = runCatching { findItems(podcast).toSet() }
                .onFailure { log.error("podcast {} ends with error", podcast.url, it) }
                .getOrNull() ?: return@measureTimedValue null

            Counter.builder("update.numberOfItem")
                .tags(
                    "url", podcast.url.toASCIIString(),
                    "id", podcast.id.toString(),
                    "type", type().key
                )
                .register(registry)
                .increment(items.size.toDouble())

            if (items.isEmpty())
                log.warn("podcast {} has no item found, potentially updater {} not working anymore", podcast.url, this::class)
            else
                log.debug("podcast {} has {} items found", podcast.url, items.size)

            return@measureTimedValue UpdatePodcastInformation(podcast, items, signature)
        }

        log.info("podcast {} ends update after {} seconds", podcast.url, duration.inWholeSeconds)

        return value
    }

    fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> = emptyList()
    fun signatureOf(url: URI): String? = ""

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

