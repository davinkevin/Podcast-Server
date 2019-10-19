package com.github.davinkevin.podcastserver.update.updaters.rss

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.manager.worker.*
import org.apache.commons.codec.digest.DigestUtils
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

class RSSUpdater(
        private val imageService: ImageService,
        private val wcb: WebClient.Builder
) : Updater {

    private val log = LoggerFactory.getLogger(RSSUpdater::class.java)

    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> = TODO("not required anymore...")
    override fun blockingSignatureOf(url: URI) = TODO("not required anymore...")

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        return fetchRss(podcast.url)
                .map { SAXBuilder().build(it.inputStream) }
                .flatMap { Mono.justOrEmpty(it.rootElement.getChild("channel")) }
                .flatMapIterable { it.getChildren("item") }
                .filter { hasEnclosure(it) }
                .flatMap { elem -> Mono.justOrEmpty(elem.getChild("thumbnail", MEDIA))
                        .map { it.getAttributeValue("url") ?: it.text }
                        .map { URI(it) }
                        .flatMap { imageService.fetchCoverInformation(it) }
                        .map { it.toCoverFromUpdate() }
                        .map { Option.just(it) }
                        .switchIfEmpty { Option.empty<CoverFromUpdate>().toMono() }
                        .map { ItemFromUpdate(
                                title = elem.getChildText("title"),
                                pubDate = getPubDate(elem),
                                description = elem.getChildText("description"),
                                cover = it.orNull(),
                                url = urlOf(elem)
                        ) }

                }
    }

    private fun hasEnclosure(item: Element) =
            item.getChild("enclosure") != null ||
                    item.getChild("origEnclosureLink", FEED_BURNER) != null

    private fun urlOf(element: Element): URI {
        val url = if (element.getChild("origEnclosureLink", FEED_BURNER) != null)
            element.getChildText("origEnclosureLink", FEED_BURNER)
        else
            element.getChild("enclosure").getAttributeValue("url")

        return URI(url.replace(" ", "+"))
    }

    private fun getPubDate(item: Element): ZonedDateTime? {
        val pubDate = item.getChildText("pubDate") ?: ""
        val date = when {
            "EDT" in pubDate -> pubDate.replace("EDT", "+0600")
            "PST" in pubDate -> pubDate.replace("PST", "+0800")
            "PDT" in pubDate -> pubDate.replace("PDT", "+0900")
            else -> pubDate
        }

        return try {
            ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e: Exception) {
            log.error("Problem during date parsing of \"{}\" caused by {}", item.getChildText("title"), e.message)
            null
        }
    }

    override fun signatureOf(url: URI): Mono<String> = fetchRss(url)
            .map { DigestUtils.md5Hex(it.inputStream) }
            .onErrorResume { "error_during_update".toMono() }

    private fun fetchRss(url: URI): Mono<ByteArrayResource> {
        return wcb
                .baseUrl(url.toASCIIString())
                .build()
                .get()
                .retrieve()
                .bodyToMono()
    }

    override fun type() = Type("RSS", "RSS")

    override fun compatibility(url: String?) =
            if((url ?: "").startsWith("http", true)) Integer.MAX_VALUE - 1
            else Integer.MAX_VALUE

    companion object {
        private val MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
        private val FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0")
    }
}
