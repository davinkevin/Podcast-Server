package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.*
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
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class RSSUpdater(
        private val imageService: ImageService,
        private val wcb: WebClient.Builder
) : Updater {

    private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")!!
    private val log = LoggerFactory.getLogger(RSSUpdater::class.java)

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        return fetchRss(podcast.url)
                .map { SAXBuilder().build(it.inputStream) }
                .flatMap { Mono.justOrEmpty(it.rootElement.getChild("channel")) }
                .flatMapMany { channel -> channel
                        .getChildren("item")
                        .toFlux()
                        .filter { hasEnclosure(it) }
                        .flatMap { elem -> Mono.justOrEmpty(elem.getChild("thumbnail", MEDIA))
                                .map { URI(it.getAttributeValue("url") ?: it.text) }
                                .flatMap {
                                    imageService.fetchCoverInformation(it)
                                            .map { cover -> Optional.of(cover.toCoverFromUpdate()) }
                                }
                                .switchIfEmpty { findPodcastCover(channel) }
                                .switchIfEmpty { Optional.empty<ItemFromUpdate.Cover>().toMono() }
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
    }

    private fun findPodcastCover(channelElement: Element): Mono<Optional<ItemFromUpdate.Cover>> {
        val rss = channelElement.getChild("image")?.getChildText("url")
        val itunes = channelElement.getChild("image", itunesNS)?.getAttributeValue("href")

        val url = rss ?: itunes ?: return Optional.empty<ItemFromUpdate.Cover>().toMono()

        return imageService.fetchCoverInformation(URI(url))
                .map { Optional.of(ItemFromUpdate.Cover(it.width, it.height, it.url)) }
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

    override fun compatibility(url: String): Int = when {
        url.startsWith("http", true) -> Integer.MAX_VALUE - 1
        else -> Integer.MAX_VALUE
    }

    companion object {
        private val MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
        private val FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0")
    }
}

private fun Element.enclosure() = this.getChild("enclosure")
