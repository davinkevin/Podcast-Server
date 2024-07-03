package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
private val FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0")
private val ITUNES_NS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")!!

class RSSUpdater(
    private val imageService: ImageService,
    private val rcb: RestClient.Builder
) : Updater {

    private val log = LoggerFactory.getLogger(RSSUpdater::class.java)

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> {
        val resource = fetchRss(podcast.url)
            ?: return emptyList()

        val xml = SAXBuilder().build(resource.inputStream)

        val channel = xml.rootElement.getChild("channel")
            ?: return emptyList()

        val rssCover = channel.getChild("image")?.getChildText("url")
        val itunesCover = channel.getChild("image", ITUNES_NS)?.getAttributeValue("href")
        val alternativeCoverURL = (rssCover ?: itunesCover)?.let(::URI)

        return channel
            .getChildren("item")
            .toList()
            .filter(::hasEnclosure)
            .map { findItem(it, alternativeCoverURL) }
    }

    private fun findItem(elem: Element, alternativeCoverURL: URI?): ItemFromUpdate {
        val coverUrl = findCover(elem) ?: alternativeCoverURL
        val cover = coverUrl?.let(imageService::fetchCoverUpdateInformation)

        return ItemFromUpdate(
            title = elem.getChildText("title"),
            pubDate = getPubDate(elem),
            description = elem.getChildText("description"),
            cover = cover,
            url = urlOf(elem),
            guid = elem.getChildText("guid"),

            length = elem.enclosure().getAttributeValue("length")?.toLong(),
            mimeType = mimeTypeOf(elem)
        )
    }

    private fun findCover(elem: Element): URI? {
        val thumbnail = elem.getChild("thumbnail", MEDIA)
            ?: return null

        return URI.create(thumbnail.getAttributeValue("url") ?: thumbnail.text)
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

    override fun signatureOf(url: URI): String {
        val resource = fetchRss(url)
            ?: return ""

        return DigestUtils.md5DigestAsHex(resource.inputStream)
    }

    private fun fetchRss(url: URI): ByteArrayResource? {
        return runCatching {
            rcb.clone()
                .baseUrl(url.toASCIIString())
                .build()
                .get()
                .retrieve()
                .body<ByteArrayResource>()
        }
            .getOrNull()
    }

    override fun type() = Type("RSS", "RSS")

    override fun compatibility(url: String): Int = when {
        url.startsWith("http", true) -> Integer.MAX_VALUE - 1
        else -> Integer.MAX_VALUE
    }
}

private fun Element.enclosure() = this.getChild("enclosure")