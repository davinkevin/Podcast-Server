package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.databind.ObjectMapper
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

class FranceTvUpdater(
    private val franceTvClient: WebClient,
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
            .map { document ->
                val jsonldTag = document.select("script[type=application/ld+json]").firstOrNull()
                    ?: error("""No <script type="application/ld+json"></script> found""")

                val jsonLd = mapper.readTree(jsonldTag.html())
                val videoObject = jsonLd.firstOrNull { it.get("@type").asText() == "VideoObject" }
                    ?: error("""No element of type VideoObject""")

                val pubDate = videoObject.get("uploadDate").asText()?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
                    ?: ZonedDateTime.now(clock)

                val item = ItemFromUpdate(
                    title = videoObject.get("name").asText().replaceFirst("Secrets d'Histoire ", ""),
                    description = videoObject.get("description").asText(),
                    length = Duration.parse(videoObject.get("duration").asText()).toLong(DurationUnit.SECONDS),
                    pubDate = pubDate,
                    url = URI("https://www.france.tv$pathUrl"),
                    cover = null,
                    mimeType = "video/mp4"
                )

                val cover = videoObject.get("thumbnailUrl").firstOrNull()?.asText()
                    ?.let(URI::create)

                item to cover
            }
            .flatMap { (item, cover) -> item.toMono().zipWith(image.fetchCoverInformationOrOption(cover)) }
            .map { (item, cover) -> item.copy(cover = cover.orNull()) }
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

private fun ImageService.fetchCoverInformationOrOption(url: URI?): Mono<Optional<ItemFromUpdate.Cover>> {
    return Mono.justOrEmpty(url)
        .flatMap { fetchCoverInformation(url!!) }
        .map { ItemFromUpdate.Cover(it.width, it.height, it.url) }
        .map { Optional.of(it) }
        .switchIfEmpty { Optional.empty<ItemFromUpdate.Cover>().toMono() }
}