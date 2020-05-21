package com.github.davinkevin.podcastserver.update.updaters.gulli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformationOrOption
import org.jsoup.Jsoup
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.time.ZonedDateTime
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 14/03/2020
 */
class GulliUpdater(
        private val wc: WebClient,
        private val image: ImageService,
        private val mapper: ObjectMapper
): Updater {

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        val path = podcast.url.toASCIIString().substringAfter("replay.gulli.fr")

        return wc.get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, "https://replay.gulli.fr/") }
                .flatMapIterable { it.select(".bloc_listing li a") }
                .map { it.attr("href") }
                .flatMap { url -> wc
                        .get()
                        .uri(url.substringAfter("replay.gulli.fr"))
                        .retrieve()
                        .bodyToMono<String>()
                        .zipWith(url.toMono())
                }
                .map { (page, url) ->
                    val html = Jsoup.parse(page, "https://replay.gulli.fr/")
                    val json = html.select("""script[type="application/ld+json"]""").html()
                    mapper.readValue<GulliItem>(json) to url
                }
                .flatMap { (item, url) -> image
                        .fetchCoverUpdateInformationOrOption(item.thumbnailUrl)
                        .map {
                            ItemFromUpdate(
                                    title = item.name,
                                    description = item.description,
                                    pubDate = ZonedDateTime.parse(item.uploadDate),
                                    url = URI(url),
                                    cover = it.orNull(),
                                    mimeType = "video/mp4"
                            )
                        }
                }
    }

    override fun signatureOf(url: URI): Mono<String> {
        val path = url.toASCIIString().substringAfter("replay.gulli.fr")

        return wc.get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, "https://replay.gulli.fr/") }
                .flatMapIterable { it.select(".bloc_listing li a") }
                .map { it.attr("href") }
                .sort()
                .reduce { t, u -> "$t, $u" }
                .map { DigestUtils.md5DigestAsHex(it.toByteArray()) }
                .switchIfEmpty("".toMono())
    }

    override fun type() = Type("Gulli", "Gulli")
    override fun compatibility(url: String?): Int =
            if ((url ?: "").contains("replay.gulli.fr")) 1
            else Integer.MAX_VALUE
}

private data class GulliItem(
        val name: String,
        val description: String,
        val thumbnailUrl: URI,
        val uploadDate: String
)
