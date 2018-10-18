package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.orElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder
import lan.dk.podcastserver.service.JsonService
import org.springframework.stereotype.Service

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@Service
class MyCanalFinder(val htmlService: HtmlService, val imageService: ImageService, val jsonService: JsonService) : Finder {

    override fun find(url: String): Podcast {
        val json = Try {
            htmlService.get(url).k()
                    .map { it.body() }
                    .flatMap { es -> es.select("script").firstOption { it.html().contains("app_config") } }
                    .map { it.html() }
                    .flatMap { extractJsonConfig(it).k() }
                    .map { jsonService.parse(it) }
                    .getOrElse { throw RuntimeException("Extraction of app_config ends up in error") }
        }

        return json
                .flatMap { Try { JsonService.to("landing.cover", MyCanalItem::class.java).apply(it) } }
                .map { toPodcast(it) }
                .orElse { json
                        .flatMap { Try { JsonService.to ("page", MyCanalPageItem::class.java).apply(it) } }
                        .map { toPodcast(it) }
                }
                .getOrElse { Podcast.DEFAULT_PODCAST }
    }

    private fun toPodcast(item: MyCanalItem) =
            Podcast().apply {
                title = item.onClick.displayName
                url = DOMAIN + item.onClick.path
                cover = imageService.getCoverFromURL(item.image)
                type = "MyCanal"
            }

    private fun toPodcast(item: MyCanalPageItem) =
            Podcast().apply {
                title = item.displayName
                url = DOMAIN + item.pathname
                cover = Cover.DEFAULT_COVER
                type = "MyCanal"
            }

    override fun compatibility(url: String?) = myCanalCompatibility(url)

    companion object {
        private const val DOMAIN = "https://www.mycanal.fr"
    }

}
