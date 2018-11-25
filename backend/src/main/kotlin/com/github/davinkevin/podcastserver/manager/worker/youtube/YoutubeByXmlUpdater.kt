package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.Option
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import org.jdom2.Element
import org.jdom2.Namespace
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by kevin on 11/09/2018
 */
@Component
@ConditionalOnProperty(name = ["podcastserver.api.youtube"], matchIfMissing = true)
class YoutubeByXmlUpdater(val jdomService: JdomService, val htmlService: HtmlService, val signatureService: SignatureService) : Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun findItems(podcast: Podcast): Set<Item> {
        log.info("Youtube Update by RSS")

        val url = playlistUrlOf(podcast.url!!)
        val parsedXml = jdomService.parse(url).k()

        val dn = parsedXml
                .map { it.rootElement }
                .map { it.namespace }
                .getOrElse { Namespace.NO_NAMESPACE }

        return parsedXml
                .map { it.rootElement }
                .map { it.getChildren("entry", it.namespace) }
                .getOrElse { listOf() }
                .map { toItem(it, dn) }
                .toSet()
    }

    private fun playlistUrlOf(url: String): String =
            if (isPlaylist(url)) PLAYLIST_RSS_BASE.format(playlistIdOf(url))
            else CHANNEL_RSS_BASE.format(channelIdOf(htmlService, url))

    private fun toItem(e: Element, dn: Namespace): Item {
        val mediaGroup = e.getChild("group", MEDIA_NAMESPACE)
        return Item().apply {
            title = e.getChildText("title", dn)
            description = mediaGroup.getChildText("description", MEDIA_NAMESPACE)

            //2013-12-20T22:30:01.000Z
            pubDate = ZonedDateTime.parse(e.getChildText("published", dn), DateTimeFormatter.ISO_DATE_TIME)

            url = urlOf(mediaGroup.getChild("content", MEDIA_NAMESPACE).getAttributeValue("url"))
            cover = coverOf(mediaGroup.getChild("thumbnail", MEDIA_NAMESPACE))
        }
    }

    private fun coverOf(thumbnail: Element?) =
            Option.fromNullable(thumbnail)
                    .map { Cover().apply {
                        url = it.getAttributeValue("url")
                        width = it.getAttributeValue("width").toInt()
                        height = it.getAttributeValue("height").toInt()
                    } }
                    .getOrElse { Cover.DEFAULT_COVER }

    private fun urlOf(embeddedVideoPage: String): String {
        val idVideo = embeddedVideoPage
                .substringAfterLast("/")
                .substringBefore("?")

        return URL_PAGE_BASE.format(idVideo)
    }

    override fun signatureOf(podcast: Podcast): String {
        val url = playlistUrlOf(podcast.url!!)
        val parsedXml = jdomService.parse(url).k()

        val dn = parsedXml
                .map { it.rootElement }
                .map { it.namespace }
                .getOrElse { Namespace.NO_NAMESPACE }

        val joinedIds = parsedXml
                .map { it.rootElement }
                .map { it.getChildren("entry", it.namespace) }
                .getOrElse { listOf() }
                .joinToString { it.getChildText("id", dn) }

        return if(joinedIds.isEmpty()) joinedIds
        else signatureService.fromText(joinedIds)
    }

    override fun type() = _type()
    override fun compatibility(url: String?) = _compatibility(url)

    companion object {
        private const val PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s"
        private const val CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s"
        private const val URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s"
        private val MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
    }

}