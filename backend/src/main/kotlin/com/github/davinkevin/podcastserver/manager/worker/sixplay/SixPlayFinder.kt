package com.github.davinkevin.podcastserver.manager.worker.sixplay

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Finder
import lan.dk.podcastserver.service.JsonService
import net.minidev.json.JSONArray
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.springframework.stereotype.Service
import javax.validation.constraints.NotEmpty

/**
 * Created by kevin on 20/12/2016 for Podcast Server
 */
@Service
class SixPlayFinder(val htmlService: HtmlService, val imageService: ImageService, val jsonService: JsonService) : Finder {

    override fun find(url: String): Podcast = htmlService.get(url)
                .map { this.htmlToPodcast(it) }
                .getOrElse(Podcast.DEFAULT_PODCAST)

    private fun htmlToPodcast(document: Document) =
            Podcast().apply {
                title = document.select("div.description-program__title").text()
                url = document.select("link[rel=canonical]").attr("href")
                description = getDescription(document.select("script"))
                cover = getCover(document)
                type = "SixPlay"
            }

    private fun getDescription(script: Elements): String? =
            script
                    .firstOption { it.html().contains("root.") }
                    .map { it.html() }
                    .map { StringUtils.substringBetween(it, " = ", "}(this));") }
                    .map { jsonService.parse(it) }
                    .map { it.read<JSONArray>("program.programsById.*.description")}
                    .flatMap { r -> r.firstOption() }
                    .map { it.toString() }
                    .getOrElse { null }

    private fun getCover(d: Document) = d.select("div.header-image__image")
            .asSequence()
            .map { it.attr("style") }
            .filterNot { it.contains("blur") }
            .flatMap { it.split(";".toRegex()).asSequence() }
            .dropWhile { it.isEmpty() }
            .firstOption { it.contains("background-image") }
            .map { StringUtils.substringBetween(it, "(", ")") }
            .map { imageService.getCoverFromURL(it) }
            .getOrElse { Cover.DEFAULT_COVER }!!

    override fun compatibility(@NotEmpty url: String?) = SixPlayUpdater.isFrom6Play(url)
}
