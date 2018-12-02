package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.UrlService
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Finder
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service

import javax.validation.constraints.NotEmpty


/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@Service
class FranceTvFinder(val htmlService: HtmlService, val imageService: ImageService) : Finder {

    override fun find(url: String) =
            htmlService.get(url)
                    .map { htmlToPodcast(it) }
                    .getOrElse(Podcast.DEFAULT_PODCAST)

    private fun htmlToPodcast(d: Document) =
            Podcast().apply {
                title = d.select("meta[property=og:title]").attr("content")
                description = d.select("meta[property=og:description]").attr("content")
                type = "FranceTv"
                cover = getCover(d)
                url = d.select("meta[property=og:url]")
                        .attr("content")
                        .addProtocolIfNecessary("https:")
            }

    private fun getCover(p: Document) =
            Option.fromNullable(p.select("meta[property=og:image]"))
                    .map { it.attr("content") }
                    .map { it.addProtocolIfNecessary("https:") }
                    .flatMap { imageService.getCoverFromURL(it).toOption() }
                    .getOrElse { Cover.DEFAULT_COVER }!!

    override fun compatibility(@NotEmpty url: String) = FranceTvUpdater.isFromFranceTv(url)
}

private fun String.addProtocolIfNecessary(protocol: String) =
        UrlService.addProtocolIfNecessary(protocol, this)
