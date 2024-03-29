package com.github.davinkevin.podcastserver.update.updaters

import com.github.davinkevin.podcastserver.service.image.CoverInformation
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.ZonedDateTime
import java.util.*

val log = LoggerFactory.getLogger(Updater::class.java)!!

interface Updater {

    fun update(podcast: PodcastToUpdate): Mono<UpdatePodcastInformation> {
        log.info("podcast {} starts update", podcast.url)
        return signatureOf(podcast.url)
                .filter { sign ->  (podcast.signature != sign)
                        .also {
                            if (it) log.debug("podcast {} has new signature {}", podcast.url, sign)
                            else log.debug("podcast {} hasn't change", podcast.url)
                        }
                }
                .flatMap { sign ->
                    findItems(podcast)
                            .collectList()
                            .map { it.toSet() }
                            .doOnNext {
                                if (it.isEmpty()) log.warn("podcast {} has no item found, potentially updater {} not working anymore", podcast.url, this::class)
                                else log.debug("podcast {} has {} items found", podcast.url, it.size)
                            }
                            .map { items -> UpdatePodcastInformation(podcast, items, sign) }
                }
                .doOnSuccess { log.info("podcast {} ends update", podcast.url) }
                .doOnError { log.error("podcast {} ends with error", podcast.url, it) }
                .onErrorResume { Mono.empty() }
    }

    fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate>
    fun signatureOf(url: URI): Mono<String>
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

val defaultItem = ItemFromUpdate(
        title = null,
        pubDate = ZonedDateTime.now(),
        length = null,
        mimeType = "",
        url = URI("http://foo.bar"),
        description = null,
        cover = null
)
