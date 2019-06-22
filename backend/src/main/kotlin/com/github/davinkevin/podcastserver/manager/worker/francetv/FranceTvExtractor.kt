package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.io.FilenameUtils
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by kevin on 24/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class FranceTvExtractor(val htmlService: HtmlService, val jsonService: JsonService) : Extractor {

    private val log = LoggerFactory.getLogger(FranceTvExtractor::class.java)!!

    override fun extract(item: Item) =
            htmlService
                    .get(item.url!!)
                    .map { it.select("script") }
                    .getOrElse {
                        log.error("No script found for item ${item.url!!}")
                        Elements()
                    }
                    .map { it.html() }
                    .firstOption { it.contains("FTVPlayerVideos") }
                    .map { it.substringAfter("=").trim(';') }
                    .flatMap { jsonService.parse(it).toOption() }
                    .map { JsonService.to(FranceTvUpdater.PAGE_ITEM).apply(it) }
                    .getOrElse { setOf() }
                    .firstOption() { it.contentId in item.url!! }
                    .map { FranceTvUpdater.CATALOG_URL.format(it.videoId) }
                    .flatMap {
                        log.info("parsing url catalog $it")
                        val parseUrl = jsonService.parseUrl(it)
                        parseUrl.k()
                    }
                    .map { JsonService.to(FranceTvVideoItem::class.java).apply(it) }
                    .map { it.url }
                    .map { DownloadingItem(item, listOf(it), item.url!!.filename().baseName() + ".mp4", null) }
                    .getOrElse { throw RuntimeException("Error during extraction of FranceTV item") }

    override fun compatibility(url: String?) = FranceTvUpdater.isFromFranceTv(url)
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class FranceTvVideoItem {

    var videos = listOf<Video>()

    val url: String
        get() {
            val onlineVideos = videos.filter { it.statut != "OFFLINE" }
            return onlineVideos
                    .firstOption { "hls_v5_os" == it.format }
                    .orElse { onlineVideos.firstOption { "m3u8-download" == it.format } }
                    .orElse { onlineVideos.firstOption { (it.secureUrl ?: "").contains("m3u8") }}
                    .map { it.secureUrl ?: it.url }
                    .getOrElse { throw RuntimeException("No video found in this FranceTvItem ") }!!
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Video {
        var format: String? = null
        var url: String? = null
        var statut: String? = null
        @JsonProperty("url_secure") var secureUrl: String? = null
    }
}

private fun String.filename() = this.substringAfterLast("/").substringBeforeLast("?")
private fun String.baseName() = FilenameUtils.getBaseName(this)
