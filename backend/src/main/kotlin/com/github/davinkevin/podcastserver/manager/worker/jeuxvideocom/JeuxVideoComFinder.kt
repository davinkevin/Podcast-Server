package com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Finder
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

/**
 * Created by kevin on 22/03/2016 for Podcast Server
 */
@Service
class JeuxVideoComFinder(val htmlService: HtmlService) : Finder {

    override fun find(url: String): Podcast {
        return htmlService.get(url).k()
                .map { htmlToPodcast(url, it) }
                .getOrElse { Podcast.DEFAULT_PODCAST }
    }

    private fun htmlToPodcast(u: String, d: Document): Podcast {
        return Podcast().apply {
            title = d.select("meta[property=og:title]").attr("content")
            description = d.select("meta[name=description]").attr("content")
            url = u
            type = "JeuxVideoCom"
        }
    }

    override fun compatibility(url: String?) = JeuxVideoComUpdater.isFromJeuxVideoCom(url)
}
