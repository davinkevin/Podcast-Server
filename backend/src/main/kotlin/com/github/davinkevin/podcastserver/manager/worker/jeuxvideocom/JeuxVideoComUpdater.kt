package com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by kevin on 18/12/14.
 */
@Component("JeuxVideoComUpdater")
class JeuxVideoComUpdater(val signatureService: SignatureService, val htmlService: HtmlService, val imageService: ImageService) : Updater {

    override fun getItems(podcast: Podcast) =
            htmlService.get(podcast.url).k()
                    .map { it.select("article") }
                    .map { htmlToItems(it) }
                    .getOrElse { setOf() }
                    .toVΛVΓ()

    private fun htmlToItems(elements: Elements) =
            elements
                    .flatMap { it.select("a").firstOption().toList() }
                    .map { generateItemFromPage(it.attr("href")) }
                    .toSet()

    private fun generateItemFromPage(videoPageUrl: String) =
            htmlService.get(JEUXVIDEOCOM_HOST + videoPageUrl).k()
                    .map { htmlToItem(it) }
                    .getOrElse { Item.DEFAULT_ITEM }!!

    private fun htmlToItem(page: Document): Item {
        val headerVideo = page.select(".header-video")
        return Item().apply {
            title = headerVideo.select("meta[itemprop=name]").attr("content")
            description = page.select(".corps-video p").text()
            url = headerVideo.select("meta[itemprop=contentUrl]").attr("content")
            pubDate = ZonedDateTime.of(LocalDateTime.parse(headerVideo.select(".date-comm time").attr("datetime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZoneId.of("Europe/Paris"))
            cover = imageService.getCoverFromURL(headerVideo.select("meta[itemprop=thumbnailUrl]").attr("content"))
        }
    }

    override fun signatureOf(podcast: Podcast) =
            htmlService.get(podcast.url).k()
                    .map { it.select("article").html() }
                    .map { signatureService.fromText(it) }
                    .getOrElse { "" }

    override fun type() = Type("JeuxVideoCom", "JeuxVideo.com")

    override fun compatibility(url: String?) = isFromJeuxVideoCom(url)

    companion object {
        const val JEUXVIDEOCOM_HOST = "http://www.jeuxvideo.com"

        fun isFromJeuxVideoCom(url: String?) =
                if ((url ?: "").contains( "jeuxvideo.com")) 1
                else Integer.MAX_VALUE
    }
}
