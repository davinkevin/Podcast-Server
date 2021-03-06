package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.extension.java.util.orNull
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.*
import java.time.Clock

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
                .flatMapIterable { it.select(".c-wall") }
                .flatMapIterable { it.select(".c-card-video") }
                .filter { it.classNames().none { c -> c.contains("unavailable") } }
                .flatMapIterable { it.select("a[href]") }
                .map { it.attr("href") }
                .flatMap { urlToItem(it) }
    }

    private fun urlToItem(pathUrl: String): Mono<ItemFromUpdate> {
        return franceTvClient
                .get()
                .uri(pathUrl)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, "https://www.france.tv/") }
                .flatMapIterable { it.select("script") }
                .map { it.html() }
                .filter { "FTVPlayerVideos" in it }
                .toMono()
                .map { it.substringAfterLast("FTVPlayerVideos = ").trim(';') }
                .flatMapIterable { mapper.readValue<Set<FranceTvPageItem>>(it) }
                .filter { it.contentId in pathUrl }
                .toMono()
                .map { "/tools/getInfosOeuvre/v2/?idDiffusion=${it.videoId}" }
                .flatMap { franceTvApi.get()
                        .uri(it)
                        .retrieve()
                        .bodyToMono<FranceTvItem>()
                }
                .flatMap { it.toMono().zipWith(image.fetchCoverInformationOrOption(it.image)) }
                .map { (franceTvItem, cover) ->
                    ItemFromUpdate(
                            title = franceTvItem.title()!!,
                            description = franceTvItem.synopsis!!,
                            pubDate = franceTvItem.pubDate(clock),
                            url = URI("https://www.france.tv$pathUrl"),
                            cover = cover.orNull(),
                            mimeType = "video/mp4"
                    )
                }
                .onErrorResume {
                    log.error("Error during fetch of $pathUrl", it)
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
                .flatMapIterable { it.select(".c-wall") }
                .flatMapIterable { it.select(".c-card-video") }
                .filter { it.classNames().none { c -> c.contains("unavailable") } }
                .flatMapIterable { it.select("a[href]") }
                .map { it.attr("href") }
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
private data class FranceTvPageItem(val contentId: String, val videoId: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FranceTvItem(
        private val titre: String? = null,
        val synopsis: String? = null,
        private val saison: String? = null,
        private val episode: String? = null,
        @field:JsonProperty("sous_titre") private val sousTitre: String? = null,
        private val diffusion: Diffusion?,
        @field:JsonProperty("image_secure") val image: URI? = null
) {

    fun title(): String? {
        val season = saison ?: ""
        val ep = episode ?: ""
        val subTitle = sousTitre ?: ""

        val seasonIsPresent     = season.isNotEmpty()
        val episodeIsPresent    = ep.isNotEmpty()
        val subTitleIsPresent   = subTitle.isNotEmpty()
        /*
         * Remove some comparison for purpose of code coverage analysis. Orders should be kept for logical purpose
         * https://github.com/jacoco/jacoco/issues/921
         */
        return when {
            seasonIsPresent    &&    !episodeIsPresent   &&    subTitleIsPresent    -> "$titre - S$season - $subTitle"
            seasonIsPresent    &&    !episodeIsPresent /*&&    subTitleIsEmpty   */ -> "$titre - S$season"
            seasonIsPresent    && /* !episodeIsEmpty     && */ subTitleIsPresent    -> "$titre - S${season}E$episode - $subTitle"
            seasonIsPresent /* &&    !episodeIsEmpty     &&    subTitleIsEmpty   */ -> "$titre - S${season}E$ep"

            /*seasonIsEmpty    && */ episodeIsPresent    &&    subTitleIsPresent    -> "$titre - E$ep - $subTitle"
            /*seasonIsEmpty    &&    episodeIsEmpty      && */ subTitleIsPresent    -> "$titre - $subTitle"
            /*seasonIsEmpty    && */ episodeIsPresent /* &&    subTitleIsEmpty   */ -> "$titre - E$ep"

            else -> titre
        }
    }

    fun pubDate(clock: Clock): ZonedDateTime {
        if (diffusion?.timestamp == null)
            return ZonedDateTime.now(clock)

        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(diffusion.timestamp), ZONE_ID)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    internal data class Diffusion(val timestamp: Long?)
}

private val ZONE_ID = ZoneId.of("Europe/Paris")


private fun ImageService.fetchCoverInformationOrOption(url: URI?): Mono<Optional<ItemFromUpdate.Cover>> {
    return Mono.justOrEmpty(url)
            .flatMap { fetchCoverInformation(url!!) }
            .map { ItemFromUpdate.Cover(it.width, it.height, it.url) }
            .map { Optional.of<ItemFromUpdate.Cover>(it) }
            .switchIfEmpty { Optional.empty<ItemFromUpdate.Cover>().toMono() }
}
