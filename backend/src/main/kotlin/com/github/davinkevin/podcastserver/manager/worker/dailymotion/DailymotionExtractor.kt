package com.github.davinkevin.podcastserver.manager.worker.dailymotion

import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.M3U8Service
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import lan.dk.podcastserver.service.JsonService
import org.jsoup.select.Elements
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by kevin on 24/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class DailymotionExtractor(val json: JsonService, val html: HtmlService, val m3u8: M3U8Service) : Extractor {

    override fun extract(item: Item): DownloadingItem {
        return html.get(item.url).k()
                .map { it.select("script") }
                .getOrElse { Elements() }
                .firstOption { "__PLAYER_CONFIG__" in it.html() }
                .flatMap { getPlayerConfig(it.html()) }
                .map { json.parse(it) }
                .map { JsonService.to("context", DailymotionContextItemExtractor::class.java).apply(it) }
                .map { it.url.replace(":videoId", toId(item.url)) }
                .flatMap { json.parseUrl(it).k() }
                .map { JsonService.to(DailymotionMetadataItemExtractor::class.java).apply(it) }
                .map { it.url }
                .map { m3u8.getM3U8UrlFormMultiStreamFile(it) }
                .map { it.substringBefore("#") }
                .map { it to getFileName(item) }
                .map { DownloadingItem(item, listOf(it.first).toVΛVΓ(), it.second, null) }
                .getOrElse { throw RuntimeException("Error during Dailymotion extraction of " + item.title + " with url " + item.url) }
    }

    fun toId(url: String) = url
            .substringAfterLast("/")
            .substringBeforeLast("?")

    override fun compatibility(url: String?) =
            if ("dailymotion.com/video" in (url ?: "")) 1
            else Integer.MAX_VALUE

    override fun getFileName(item: Item) = super.getFileName(item) + ".mp4"

    private fun getPlayerConfig(page: String): Option<String> {
        val startToken = "var __PLAYER_CONFIG__ = "
        val endToken = "};"

        if (!page.contains(startToken) || !page.contains(endToken)) {
            throw RuntimeException("Structure of Dailymotion page changed")
        }

        val begin = page.indexOf(startToken)
        val end = page.indexOf(endToken, begin)
        return Some(page.substring(begin + startToken.length - 1, end + 1))
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class DailymotionMetadataItemExtractor(@JsonProperty("stream_chromecast_url") var url: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private class DailymotionContextItemExtractor(@JsonProperty("metadata_template_url") var url: String)

