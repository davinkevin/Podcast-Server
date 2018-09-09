package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import io.vavr.collection.Stream
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Finder
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

/**
 * Created by kevin on 22/02/15
 */
@Service
class YoutubeFinder(val htmlService: HtmlService) : Finder {

    override fun find(url: String): Podcast {
        return htmlService.get(url).k()
                .map { podcastFromHtml(url, it) }
                .getOrElse { Podcast.DEFAULT_PODCAST }
    }

    private fun podcastFromHtml(anUrl: String, p: Document) =
            Podcast().apply {
                url = anUrl
                type = "Youtube"
                title = p.meta("title")
                description = p.meta("description")
                cover = getCover(p)
            }

    private fun getCover(page: Document): Cover {
        val coverUrl = page
                .select("img.channel-header-profile-image")
                .attr("src")

        return when(coverUrl.isEmpty()) {
            true -> Cover.DEFAULT_COVER
            false -> Cover().apply { url = coverUrl; height = 200; width = 200 }
        }
    }

    override fun compatibility(url: String?) =
            if (isYoutubeUrl(url)) 1
            else Integer.MAX_VALUE

    private fun isYoutubeUrl(url: String?) =
            Stream.of("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                    .any { url?.contains(it) == true }
}

private fun Document.meta(s: String) =
        this.select("meta[name=$s]").attr("content")
