package com.github.davinkevin.podcastserver.manager.worker.sixplay

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import lan.dk.podcastserver.service.JsonService
import net.minidev.json.JSONArray
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.validation.constraints.NotEmpty

/**
 * Created by kevin on 20/12/2016 for Podcast Server
 */
@Service
class SixPlayFinder(val htmlService: HtmlService, val imageService: ImageService, val jsonService: JsonService) : Finder {

    private var log = LoggerFactory.getLogger(SixPlayFinder::class.java)

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
                    .map { it.replace("\n".toRegex(), "") }
                    .map { it.substringBetween(" = ", "}(this));")!! }
                    .map { it.cleanForJsonSerialization() }
                    .map { jsonService.parse(it) }
                    .map { it.read<JSONArray>("program.programsById.*.description") }
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

internal fun String?.substringBetween(start: String?, close: String?): String? {
    if (this == null || start == null || close == null) {
        return null
    }
    val s = this.indexOf(start)
    if (s != -1) {
        val end = this.indexOf(close, s + start.length)
        if (end != -1) {
            return this.substring(s + start.length, end)
        }
    }
    return null
}

internal fun String.cleanForJsonSerialization() = this
    .replace("\\\"", "\"")
    .replace("\\\\\"", "\\\"")
    .trim(';', '"')
