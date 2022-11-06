package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.time.Clock
import java.time.OffsetDateTime
import java.util.*

class FranceTvUpdater(
    private val franceTvClient: WebClient,
    private val franceTvApi: WebClient,
    private val image: ImageService,
    private val mapper: ObjectMapper,
    private val clock: Clock
): Updater {

    private val log = LoggerFactory.getLogger(FranceTvUpdater::class.java)

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {

        val url = podcast.url.toASCIIString()

        log.debug("Fetch $url")

        val replay = replayUrl(podcast.url)

        return franceTvClient
            .get()
            .uri(replay)
            .retrieve()
            .bodyToMono<String>()
            .map { Jsoup.parse(it, url) }
            .flatMapIterable { it.select(".c-wall__item > [data-video-id]") }
            .filter { !it.html().contains("indisponible") }
            .map { it.select("a[href]").attr("href") }
            .flatMap { urlToItem(it) }
    }

    private fun urlToItem(pathUrl: String): Mono<ItemFromUpdate> {
        return franceTvClient
            .get()
            .uri(pathUrl)
            .retrieve()
            .bodyToMono<String>()
            .map { Jsoup.parse(it, "https://www.france.tv/") }
            .flatMap { document ->
                val description = document.select("meta[property=og:description]").attr("content")

                val pageItems = document.select("script")
                    .map { it.html() }
                    .firstOrNull { "FTVPlayerVideos" in it }
                    ?.substringAfterLast("FTVPlayerVideos = ")
                    ?.trim(';')
                    ?.let { mapper.readValue<Set<FranceTvPageItem>>(it) } ?: emptySet()

                pageItems
                    .firstOrNull { it.contentId in pathUrl }
                    ?.copy(description = description)
                    ?.toMono() ?: Mono.empty()
            }
            .flatMap { pageItem ->
                franceTvApi.get()
                    .uri { it.path("v1/videos/${pageItem.videoId}")
                        .queryParam("country_code", "FR")
                        .queryParam("device_type", "desktop")
                        .queryParam("browser", "chrome")
                        .build()
                    }
                    .retrieve()
                    .bodyToMono<FranceTvItemV2>()
                    .map { it.copy(externalDescription = pageItem.description) }
            }
            .flatMap {
                it.toMono().zipWith(image.fetchCoverInformationOrOption(it.coverUri()))
            }
            .map { (franceTvItem, cover) ->
                ItemFromUpdate(
                    title = franceTvItem.title(),
                    description = franceTvItem.description(),
                    pubDate = franceTvItem.pubDate(clock).toZonedDateTime(),
                    url = URI("https://www.france.tv$pathUrl"),
                    cover = cover.orNull(),
                    mimeType = "video/mp4"
                )
            }
            .onErrorResume {
                val message = "Error during fetch of $pathUrl"
                log.error(message)
                log.debug(message, it)
                Mono.empty()
            }
    }

    override fun signatureOf(url: URI): Mono<String> {

        val replay = replayUrl(url)

        return franceTvClient
            .get()
            .uri(replay)
            .retrieve()
            .bodyToMono<String>()
            .map { Jsoup.parse(it, url.toASCIIString()) }
            .flatMapIterable { it.select(".c-wall__item > [data-video-id]") }
            .filter { !it.html().contains("indisponible") }
            .map { it.select("a[href]").attr("href") }
            .sort()
            .reduce { t, u -> """$t-$u""" }
            .map { DigestUtils.md5DigestAsHex(it.toByteArray()) }
            .switchIfEmpty("".toMono())
    }

    override fun type() = Type("FranceTv", "France•tv")

    override fun compatibility(url: String): Int = when {
        "www.france.tv" in url -> 1
        else -> Integer.MAX_VALUE
    }

    private fun replayUrl(url: URI): String {
        return "${url.toASCIIString()}/toutes-les-videos/"
            .substringAfter("https://www.france.tv/")

    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
private data class FranceTvPageItem(val contentId: String, val videoId: String, val description: String = "")

private fun ImageService.fetchCoverInformationOrOption(url: URI?): Mono<Optional<ItemFromUpdate.Cover>> {
    return Mono.justOrEmpty(url)
        .flatMap { fetchCoverInformation(url!!) }
        .map { ItemFromUpdate.Cover(it.width, it.height, it.url) }
        .map { Optional.of(it) }
        .switchIfEmpty { Optional.empty<ItemFromUpdate.Cover>().toMono() }
}

private data class FranceTvItemV2 (val meta: Meta? = null, val externalDescription: String?) {

    fun title(): String {
        if (meta?.additional_title != null) {
            return """${meta.title!!} - ${meta.additional_title}"""
        }
        return meta?.title!!
    }
    fun description(): String = externalDescription ?: meta?.additional_title ?: ""
    fun pubDate(clock: Clock): OffsetDateTime = meta?.broadcasted_at?.let(OffsetDateTime::parse) ?: OffsetDateTime.now(clock)
    fun coverUri(): URI = meta?.image_url!!
}

data class Meta (
    val title: String? = null,
    val additional_title: String? = null,
    val broadcasted_at: String? = null,
    val image_url: URI? = null
)
