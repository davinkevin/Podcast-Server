package com.github.davinkevin.podcastserver.manager.worker.rss

import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.manager.worker.*
import com.github.davinkevin.podcastserver.service.CoverInformation
import org.jdom2.Element
import org.jdom2.Namespace
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.Exception
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class RSSUpdater(val signatureService: SignatureService, val jdomService: JdomService, val imageService: ImageService) : Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun findItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> =
            jdomService.parse(podcast.url.toASCIIString())
                    .k()
                    .flatMap { it.rootElement.getChild("channel").toOption() }
                    .map { it.getChildren("item") }
                    .getOrElse { listOf() }
                    .filter { hasEnclosure(it) }
                    .map { extractItem(it) }
                    .toSet()

    private fun hasEnclosure(item: Element) =
            item.getChild("enclosure") != null ||
                    item.getChild("origEnclosureLink", FEED_BURNER) != null

    private fun extractItem(item: Element) =
            ItemFromUpdate(
                title = item.getChildText("title"),
                pubDate = getPubDate(item),
                description = item.getChildText("description"),
                cover = coverOf(item)?.toCoverFromUpdate(),
                url = URI(urlOf(item))
            )

    private fun urlOf(element: Element) =
            if (element.getChild("origEnclosureLink", FEED_BURNER) != null)
                element.getChildText("origEnclosureLink", FEED_BURNER)
            else
                element.getChild("enclosure").getAttributeValue("url")

    private fun coverOf(element: Element): CoverInformation? {
        return element.getChild("thumbnail", MEDIA)
                .toOption()
                .map { it.getAttributeValue("url").toOption().getOrElse { it.text } }
                .flatMap { imageService.fetchCoverInformation(it).toOption() }
                .getOrElse { null }
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

    override fun signatureOf(url: URI) = signatureService.fromUrl(url.toASCIIString())

    override fun type() = Type("RSS", "RSS")

    override fun compatibility(url: String?) =
            if((url ?: "").startsWith("http", true)) Integer.MAX_VALUE - 1
            else Integer.MAX_VALUE

    companion object {
        private val MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
        private val FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0")
    }
}
