package com.github.davinkevin.podcastserver.manager.worker.gulli

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

/**
 * Created by kevin on 05/10/2016 for Podcast Server
 */
@Component
class GulliUpdater(val signatureService: SignatureService, val htmlService: HtmlService, val imageService: ImageService) : Updater {

    override fun findItems(podcast: Podcast) =
            htmlService.get(podcast.url).k()
                    .map { it.select("div.all-videos ul li.col-md-3") }
                    .getOrElse{ Elements() }
                    .flatMap { findDetailsInFromPage(it).toList() }
                    .toSet()

    private fun findDetailsInFromPage(e: Element) =
            e.select("a").firstOption()
                    .map { it.attr("href") }
                    .flatMap { htmlService.get(it).k() }
                    .flatMap { it.select(".bloc_streaming").firstOption() }
                    .flatMap { htmlToItem(it, coverOf(e)) }

    private fun htmlToItem(block: Element, aCover: Cover) =
            block.select("script")
                    .firstOption { it.html().contains("iframe") }
                    .map { it.html() }
                    .flatMap { FRAME_EXTRACTOR.on(it).group(1).k() }
                    .map { anUrl ->
                        Item().apply {
                            title = block.select(".episode_title").text()
                            description = block.select(".description").text()
                            url = anUrl
                            pubDate = ZonedDateTime.now()
                            cover = aCover
                        }
                    }

    private fun coverOf(block: Element) =
            Option(block)
                    .map { it.select("img").attr("src") }
                    .flatMap { imageService.getCoverFromURL(it).toOption() }
                    .getOrElse { Cover.DEFAULT_COVER }

    override fun signatureOf(podcast: Podcast)=
            htmlService.get(podcast.url).k()
                    .flatMap { it.select("div.all-videos ul").firstOption() }
                    .map { it.html() }
                    .map { signatureService.fromText(it) }
                    .getOrElse { StringUtils.EMPTY }

    override fun type() = Type("Gulli", "Gulli")

    override fun compatibility(url: String?): Int =
            if ((url ?: "").contains("replay.gulli.fr")) 1
            else Integer.MAX_VALUE

    companion object {
        private val FRAME_EXTRACTOR = from(".*\\.html\\(.*<iframe.* src=\"([^\"]*)\".*")
    }
}
