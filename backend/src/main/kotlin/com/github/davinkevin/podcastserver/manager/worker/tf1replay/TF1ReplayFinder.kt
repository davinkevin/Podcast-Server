package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

/**
 * Created by kevin on 20/07/2016.
 */
@Service
class TF1ReplayFinder(val htmlService: HtmlService, val imageService: ImageService) : Finder {

    override fun find(url: String): Podcast {
        return htmlService.get(url).k()
                .map { toPodcast(it, url) }
                .getOrElse { Podcast.DEFAULT_PODCAST }
    }

    private fun toPodcast(d: Document, anUrl: String) =
            Podcast().apply {
                title = d.select("meta[property=og:title]").attr("content")
                description = d.select("meta[property=og:description]").attr("content")
                url = anUrl
                cover = getCover(d)
                type = "TF1Replay"

            }

    private fun getCover(p: Document) =
            PICTURE_EXTRACTOR.on(p.select(".focalImg style").html()).group(1).k()
                    .orElse { p.select("meta[property=og:image]").attr("content").toOption() }
                    .map { toUrl(it) }
                    .flatMap { imageService.getCoverFromURL(it).toOption() }
                    .getOrElse { Cover.DEFAULT_COVER }

    private fun toUrl(url: String): String = when {
        url.startsWith("//") -> "http:$url"
        else -> url
    }


    override fun compatibility(url: String?) =
            if (StringUtils.contains(url, "www.tf1.fr")) 1
            else Integer.MAX_VALUE

    companion object {
        private val PICTURE_EXTRACTOR = from("url\\(([^)]+)\\).*")
    }
}
