package com.github.davinkevin.podcastserver.manager.worker.rss

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import org.jdom2.Element
import org.jdom2.Namespace
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class RSSUpdater(val signatureService: SignatureService, val jdomService: JdomService, val imageService: ImageService) : Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun getItems(podcast: Podcast) =
            jdomService.parse(podcast.url)
                    .k()
                    .flatMap { it.rootElement.getChild("channel").toOption() }
                    .map { it.getChildren("item") }
                    .getOrElse { listOf() }
                    .filter { hasEnclosure(it) }
                    .map { extractItem(it) }
                    .toSet()
                    .toVΛVΓ()

    private fun hasEnclosure(item: Element) =
            item.getChild("enclosure") != null ||
                    item.getChild("origEnclosureLink", FEED_BURNER) != null

    private fun extractItem(item: Element) =
            Item().apply {
                title = item.getChildText("title")
                pubDate = getPubDate(item)
                description = item.getChildText("description")
                mimeType = item.getChild("enclosure").getAttributeValue("type")
                length = lengthOf(item)
                cover = coverOf(item)
                url = urlOf(item)
            }

    private fun lengthOf(item: Element) =
            if (!item.getChild("enclosure").getAttributeValue("length").isNullOrEmpty())
                item.getChild("enclosure").getAttributeValue("length").toLong()
            else
                0L

    private fun urlOf(element: Element) =
            if (element.getChild("origEnclosureLink", FEED_BURNER) != null)
                element.getChildText("origEnclosureLink", FEED_BURNER)
            else
                element.getChild("enclosure").getAttributeValue("url")

    private fun coverOf(element: Element): Cover {
        return element.getChild("thumbnail", MEDIA)
                .toOption()
                .map { it.getAttributeValue("url").toOption().getOrElse { it.text } }
                .flatMap { imageService.getCoverFromURL(it).toOption() }
                .getOrElse { Cover.DEFAULT_COVER }!!
    }

    private fun getPubDate(item: Element): ZonedDateTime? {
        val pubdate = item.getChildText("pubDate") ?: ""
        val date = when {
            "EDT" in pubdate -> pubdate.replace("EDT".toRegex(), "+0600")
            "PST" in pubdate -> pubdate.replace("PST".toRegex(), "+0800")
            "PDT" in pubdate -> pubdate.replace("PDT".toRegex(), "+0900")
            else -> pubdate
        }

        return Try { ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME) }
                .getOrElse {
                    log.error("Problem during date parsing of \"{}\" caused by {}", item.getChildText("title"), it.message)
                    null
                }
    }

    override fun signatureOf(podcast: Podcast) = signatureService.fromUrl(podcast.url)

    override fun type() = Type("RSS", "RSS")

    override fun compatibility(url: String?) =
            if((url ?: "").startsWith("http", true)) Integer.MAX_VALUE - 1
            else Integer.MAX_VALUE

    companion object {
        private val MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
        private val FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0")
    }
}
