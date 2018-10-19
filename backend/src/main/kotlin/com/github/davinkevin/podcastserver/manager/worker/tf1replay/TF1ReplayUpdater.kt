package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.getOrElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.service.JsonService
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import javax.validation.constraints.NotNull

/**
 * Created by kevin on 20/07/2016
 */
@Component
class TF1ReplayUpdater(val signatureService: SignatureService, val htmlService: HtmlService, val imageService: ImageService, val jsonService: JsonService) : Updater {

    override fun getItems(podcast: Podcast) =
            htmlFromStandardOrReplay(podcast.url)
                    .map { toItem(it) }
                    .toSet()
                    .toVΛVΓ()

    private fun toItem(e: Element): Item {
        val link = e.select(".videoLink").attr("href")
        val anUrl = if (link.startsWith("/")) "$DOMAIN$link" else link
        return Item().apply {
            title = findTitle(e)
            description = e.select("p.stitle").text()
            pubDate = findDate(anUrl)
            url = anUrl
            cover = findCover(e)
        }
    }

    private fun findTitle(v: Element): String {
        val text = v.select("p.title").text()
        return when {
            text.contains(" - ") -> text.substringAfter(" - ").trim { it <= ' ' }
            else -> text
        }
    }

    private fun findCover(e: Element) =
            e.select("source")
                    .first()
                    .attr("data-srcset")
                    .split(",")
                    .lastOrNull { it.isNotEmpty() }
                    .toOption()
                    .flatMap { imageService.getCoverFromURL(SCHEME_DEFAULT + it).toOption() }
                    .getOrElse { Cover.DEFAULT_COVER }

    private fun findDate(url: String) =
            htmlService.get(url).k()
                    .map { it.select("script[type=application/ld+json]") }
                    .getOrElse { Elements() }
                    .firstOption()
                    .map { it.html() }
                    .map { jsonService.parse(it) }
                    .map { JsonService.to(TF1ReplayUpdaterItem::class.java).apply(it) }
                    .flatMap { it.uploadDate.toOption() }
                    .getOrElse { ZonedDateTime.now() }

    override fun signatureOf(podcast: Podcast): String {
        val v = htmlFromStandardOrReplay(podcast.url).html()
        return  if (v.isNotEmpty()) signatureService.fromText(v)
        else v
    }

    private fun htmlFromStandardOrReplay(url: String): Elements {
        val replays = getElementsFrom(url, REPLAY_CATEGORY)
        if (replays.isNotEmpty()) {
            return replays
        }

        return getElementsFrom(url, ALL_CATEGORY)
    }

    private fun getElementsFrom(url: String, @NotNull inCategory: String): Elements {
        return CHANNEL_PROGRAM_EXTRACTOR.on(url).groups().k()
                .map { AJAX_URL_FORMAT.format(it[0], it[1], inCategory) }
                .flatMap { jsonService.parseUrl(it).k() }
                .map { JsonService.to(TF1ReplayUpdaterResponse::class.java).apply(it) }
                .map { it.html }
                .map { htmlService.parse(it) }
                .map { it.select(".video") }
                .getOrElse { Elements() }
                .asSequence()
                .filter { it.attr("data-id").isNotEmpty() }
                .filter { isReplayOrVideo(it) }
                .toCollection(Elements())
    }

    private fun isReplayOrVideo(element: Element) =
            element.select(".uptitle strong").text().toLowerCase() in TYPES

    override fun type() = Type("TF1Replay", "TF1 Replay")

    override fun compatibility(url: String?) =
            if ("www.tf1.fr" in (url ?: "")) 1
            else Integer.MAX_VALUE

    companion object {
        private const val AJAX_URL_FORMAT = "http://www.tf1.fr/ajax/%s/%s/videos?filter=%s"
        private const val SCHEME_DEFAULT = "https:"
        private const val DOMAIN = "https://www.tf1.fr"
        private const val REPLAY_CATEGORY = "replay"
        private const val ALL_CATEGORY = "all"

        private val CHANNEL_PROGRAM_EXTRACTOR = from("[^:]+://www.tf1.fr/([^/]+)/([^/]+)/videos.*")
        private val TYPES = setOf("replay", "vidéo", "")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TF1ReplayUpdaterResponse(val html: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TF1ReplayUpdaterItem(val uploadDate: ZonedDateTime?)