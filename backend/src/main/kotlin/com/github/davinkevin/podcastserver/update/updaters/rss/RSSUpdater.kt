package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.extension.java.util.orNull
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.*

class RSSUpdater(
        private val imageService: ImageService,
        private val wcb: WebClient.Builder
) : Updater {

    private val log = LoggerFactory.getLogger(RSSUpdater::class.java)

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
                        .map { Optional.of(it) }
                        .switchIfEmpty { Optional.empty<CoverFromUpdate>().toMono() }
                        .map {
                            val enclosure = elem.enclosure()
                            ItemFromUpdate(
                                    title = elem.getChildText("title"),
                                    pubDate = getPubDate(elem),
                                    description = elem.getChildText("description"),
                                    cover = it.orNull(),
                                    url = urlOf(elem),

                                    length = enclosure.getAttributeValue("length")?.toLong(),
                                    mimeType = mimeTypeOf(elem)
                            )
                        }

                }
    }

    private fun hasEnclosure(item: Element) = item.enclosure() != null

    private fun urlOf(element: Element): URI {
        val url = if (element.getChild("origEnclosureLink", FEED_BURNER) != null)
            element.getChildText("origEnclosureLink", FEED_BURNER)
        else
            element.enclosure().getAttributeValue("url")

        return URI(url.replace(" ", "+"))
    }

    private fun mimeTypeOf(elem: Element): String {
        val type = elem.enclosure().getAttributeValue("type")
        return if (!type.isNullOrEmpty()) type else "unknown/unknown"
    }

    private fun getPubDate(item: Element): ZonedDateTime {
        val pubDate = item.getChildText("pubDate") ?: ""
        val date = when {
            "EDT" in pubDate -> pubDate.replace("EDT", "+0600")
            "PST" in pubDate -> pubDate.replace("PST", "+0800")
            "PDT" in pubDate -> pubDate.replace("PDT", "+0900")
            else -> pubDate
        }

        return Result.runCatching {
            ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME)
        } .onFailure {
            log.error("Problem during date parsing of \"{}\" caused by {}", item.getChildText("title"), it.message)
        }
                .getOrDefault(ZonedDateTime.now())
    }

    override fun signatureOf(url: URI): Mono<String> = fetchRss(url)
            .map { DigestUtils.md5DigestAsHex(it.inputStream) }
            .onErrorResume {
                log.error("error during update", it)
                "error_during_update".toMono()
            }

    private fun fetchRss(url: URI): Mono<ByteArrayResource> {
        return wcb
                .clone()
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

private fun Element.enclosure() = this.getChild("enclosure")
