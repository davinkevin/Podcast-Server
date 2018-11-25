package com.github.davinkevin.podcastserver.manager.worker.gulli

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service
import javax.validation.constraints.NotEmpty

/**
 * Created by kevin on 04/10/2016 for Podcast Server
 */
@Service
class GulliFinder(val htmlService: HtmlService, val imageService: ImageService) : Finder {

    override fun find(url: String): Podcast {
        return htmlService.get(url).k()
                .map { htmlToPodcast(it) }
                .getOrElse { Podcast.DEFAULT_PODCAST }
    }

    private fun htmlToPodcast(d: Document) =
            Podcast().apply {
                title = d.select("ol.breadcrumb li.active").first().text()
                cover = coverOf(d)
                description = d.select("meta[property=og:description]").attr("content")
                url = d.select("meta[property=og:url]").attr("content")
                type = "Gulli"
            }

    private fun coverOf(d: Document): Cover {
        val pageUrl = d.select("meta[property=og:url]").attr("content")

        return d.select(COVER_SELECTOR.format(pageUrl))
                .firstOption()
                .map { it.attr("src") }
                .map { imageService.getCoverFromURL(it) }
                .getOrElse { Cover.DEFAULT_COVER }!!
    }

    override fun compatibility(@NotEmpty url: String?) =
            if ((url ?: "").contains("replay.gulli.fr")) 1
            else Integer.MAX_VALUE

    companion object {
        private const val COVER_SELECTOR = "div.program_gullireplay a[href=%s] img"
    }
}
