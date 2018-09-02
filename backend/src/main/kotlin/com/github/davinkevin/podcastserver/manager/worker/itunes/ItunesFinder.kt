package com.github.davinkevin.podcastserver.manager.worker.itunes

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Finder
import lan.dk.podcastserver.manager.worker.rss.RSSFinder
import lan.dk.podcastserver.service.JsonService
import org.springframework.stereotype.Service

/**
 * Created by kevin on 12/05/2018
 */
@Service
class ItunesFinder(val rssFinder: RSSFinder, val jsonService: JsonService) : Finder {

    override fun find(url: String) =
            ARTIST_ID.on(url)
                    .group(1).k()
                    .map { "$ITUNES_API$it" }
                    .flatMap { jsonService.parseUrl(it).k() }
                    .map { JsonService.to("results[0].feedUrl", String::class.java).apply(it) }
                    .map { rssFinder.find(it) }
                    .getOrElse { Podcast.DEFAULT_PODCAST }

    override fun compatibility(url: String?) =
            if ((url ?: "").contains("itunes.apple.com")) 1
            else Integer.MAX_VALUE

    companion object {
        private const val ITUNES_API = "https://itunes.apple.com/lookup?id="
        private val ARTIST_ID = from(".*id=?([\\d]+).*")
    }
}
