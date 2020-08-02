package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.update.updaters.CoverFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jsoup.Jsoup
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by kevin on 15/03/2020
 */
class YoutubeByXmlUpdater(
        private val wc: WebClient
): Updater {

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        return fetchXml(podcast.url)
                .flatMapMany { text ->
                    val xml = SAXBuilder().build(text.inputStream)
                    val dn = xml.rootElement.namespace
                    xml
                            .rootElement
                            .getChildren("entry", dn)
                            .toFlux()
                            .map { toItem(it, dn) }
                }
    }

    private fun toItem(e: Element, dn: Namespace): ItemFromUpdate {
        val mediaGroup = e.getChild("group", MEDIA_NAMESPACE)
        val idVideo = mediaGroup.getChild("content", MEDIA_NAMESPACE)
                .getAttributeValue("url")
                .substringAfterLast("/")
                .substringBefore("?")

        val thumbnail = mediaGroup.getChild("thumbnail", MEDIA_NAMESPACE)

        return ItemFromUpdate(
                title = e.getChildText("title", dn),
                description = mediaGroup.getChildText("description", MEDIA_NAMESPACE),

                //2013-12-20T22:30:01.000Z
                pubDate = ZonedDateTime.parse(e.getChildText("published", dn), DateTimeFormatter.ISO_DATE_TIME),

                url = URI("https://www.youtube.com/watch?v=$idVideo"),
                cover = CoverFromUpdate(
                        url = URI(thumbnail.getAttributeValue("url")),
                        width = thumbnail.getAttributeValue("width").toInt(),
                        height = thumbnail.getAttributeValue("height").toInt()
                ),
                mimeType = "video/webm"
        )
    }

    override fun signatureOf(url: URI): Mono<String> {
        return fetchXml(url)
                .flatMapIterable { text ->
                    val xml = SAXBuilder().build(text.inputStream)
                    val dn = xml.rootElement.namespace
                    xml
                            .rootElement
                            .getChildren("entry", dn)
                            .map { it.getChildText("id", dn) }
                }
                .sort()
                .reduce { t, u -> "$t, $u" }
                .map { DigestUtils.md5DigestAsHex(it.toByteArray()) }
                .switchIfEmpty("".toMono())
    }

    private fun fetchXml(url: URI): Mono<ByteArrayResource> {
        return queryParamsOf(url)
                .flatMap { (key, value) -> wc
                        .get()
                        .uri { it
                                .path("/feeds/videos.xml")
                                .queryParam(key, value)
                                .build()
                        }
                        .retrieve()
                        .bodyToMono<ByteArrayResource>()
                }
    }

    private fun queryParamsOf(url: URI): Mono<Pair<String, String>> {
        val stringUrl = url.toASCIIString()

        if (isPlaylist(url)) {
            val playlistId = stringUrl.substringAfter("list=")
            return ("playlist_id" to playlistId).toMono()
        }

        val path = stringUrl.substringAfterLast("https://www.youtube.com")
        return wc
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, "https://www.youtube.com") }
                .flatMap { Mono.justOrEmpty(it.select("meta[itemprop=channelId]").firstOrNull()) }
                .map { "channel_id" to it.attr("content") }
    }

    override fun type() = type
    override fun compatibility(url: String): Int = youtubeCompatibility(url)

    companion object {
        private const val PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s"
        private const val CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s"
        private const val URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s"
        private val MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
    }
}
