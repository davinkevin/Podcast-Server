package com.github.davinkevin.podcastserver.update.updaters.dailymotion

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformationOrOption
import com.github.davinkevin.podcastserver.utils.MatcherExtractor
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 13/03/2020
 */
class DailymotionUpdater(
        private val wc: WebClient,
        private val image: ImageService
): Updater {

    override fun signatureOf(url: URI): Mono<String> {
        val userName = USER_NAME_EXTRACTOR.on(url.toASCIIString()).group(1) ?: error("username not found")

        return wc
                .get()
                .uri("/user/{userName}/videos?fields=id", userName)
                .retrieve()
                .bodyToMono<DailymotionResult>()
                .flatMapIterable { it.list }
                .map { it.id }
                .sort()
                .reduce { t, u -> """$t, $u""" }
                .map { DigestUtils.md5DigestAsHex(it.toByteArray()) }
                .switchIfEmpty("".toMono())
    }

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        val userName = USER_NAME_EXTRACTOR.on(podcast.url.toASCIIString()).group(1) ?: error("username not found")

        return wc
                .get()
                .uri("/user/{userName}/videos?fields=created_time,description,id,thumbnail_720_url,title", userName)
                .retrieve()
                .bodyToMono<DailymotionDetailsResult>()
                .flatMapIterable { it.list }
                .flatMap { image.fetchCoverUpdateInformationOrOption(it.cover).zipWith(it.toMono()) }
                .map { (cover, item) ->
                    ItemFromUpdate(
                            url = URI("https://www.dailymotion.com/video/${item.id}"),
                            cover = cover.orNull(),
                            title = item.title,
                            pubDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(item.creationDate!!), ZoneId.of("Europe/Paris")),
                            description = item.description!!,
                            mimeType = "video/mp4"
                    )
                }
    }

    override fun type() = Type("Dailymotion", "Dailymotion")
    override fun compatibility(url: String?) =
            if ("www.dailymotion.com" in (url ?: "")) 1
            else Integer.MAX_VALUE
}

// http://www.dailymotion.com/karimdebbache
private val USER_NAME_EXTRACTOR = MatcherExtractor.from("^.+dailymotion.com/(.*)")

private class DailymotionDetailsResult(val list: Set<DailymotionVideoDetail> = emptySet()) {
    class DailymotionVideoDetail(
            val id: String,
            val title: String,
            val description: String? = null,
            @JsonProperty("created_time") val creationDate: Long? = null,
            @JsonProperty("thumbnail_720_url") val cover: URI? = null
    )
}

private class DailymotionResult(val list: Set<DailymotionUpdaterVideoDetail> = emptySet()) {
    class DailymotionUpdaterVideoDetail(val id: String)
}
