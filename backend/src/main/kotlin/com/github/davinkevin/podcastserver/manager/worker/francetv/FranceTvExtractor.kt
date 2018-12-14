package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.getOrElse
import arrow.core.orElse
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.downloader.DownloadingItem
import lan.dk.podcastserver.manager.worker.Extractor
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by kevin on 24/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class FranceTvExtractor(val htmlService: HtmlService, val jsonService: JsonService) : Extractor {

    override fun extract(item: Item) =
            htmlService.get(item.url).k()
                    .flatMap { it.select("#player").firstOption() }
                    .map { it.attr("data-main-video") }
                    .map { CATALOG_URL.format(it) }
                    .flatMap { jsonService.parseUrl(it).k() }
                    .map { JsonService.to(FranceTvItem::class.java).apply(it) }
                    .map { it.url }
                    .map { DownloadingItem(item, listOf(it).toVΛVΓ(), item.url.filename().baseName() + ".mp4", null) }
                    .getOrElse { throw RuntimeException("Error during extraction of FranceTV item") }


    override fun compatibility(url: String?) = FranceTvUpdater.isFromFranceTv(url)

    companion object {
        private const val CATALOG_URL = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class FranceTvItem {

        var videos = listOf<Video>()

        val url: String
            get() = videos
                    .firstOption { "hls_v5_os" == it.format }
                    .orElse { videos.firstOption { "m3u8-download" == it.format } }
                    .orElse { videos.firstOption { (it.secureUrl ?: "").contains("m3u8") }}
                    .map { it.secureUrl ?: it.url }
                    .getOrElse { throw RuntimeException("No video found in this FranceTvItem ") }!!

        @JsonIgnoreProperties(ignoreUnknown = true)
        private class Video {
            var format: String? = null
            var url: String? = null
            @JsonProperty("url_secure") var secureUrl: String? = null
        }
    }
}

private fun String.filename() = this.substringAfterLast("/").substringBeforeLast("?")
private fun String.baseName() = FilenameUtils.getBaseName(this)