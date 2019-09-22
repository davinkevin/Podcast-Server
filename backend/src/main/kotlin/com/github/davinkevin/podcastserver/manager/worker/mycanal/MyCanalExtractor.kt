package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.service.JsonService
import org.jsoup.select.Elements
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Created by kevin on 24/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class MyCanalExtractor(val htmlService: HtmlService, val jsonService: JsonService) : Extractor {

    override fun extract(item: DownloadingItem): DownloadingInformation =
            htmlService.get(item.url.toASCIIString()).k()
                    .map { it.body() }
                    .map { it.select("script") }
                    .getOrElse { Elements() }
                    .firstOption { it.html().contains("__data") }
                    .map { it.html() }
                    .flatMap { extractJsonConfig(it).k() }
                    .map { jsonService.parse(it) }
                    .map { JsonService.to("detailPage.body.contentID", String::class.java).apply(it) }
                    .flatMap { jsonService.parseUrl(URL_DETAILS.format(it)).k() }
                    .map { JsonService.to("MEDIA.VIDEOS", MyCanalVideoItem::class.java).apply(it) }
                    .map { DownloadingInformation(item, listOf(it.hls), getFileName(item.url), null) }
                    .getOrElse { throw RuntimeException("Error during extraction of ${item.title} at url ${item.url}") }

    override fun getFileName(url: URI): String = "${super.getFileName(url)}.mp4"

    override fun compatibility(url: URI): Int = myCanalCompatibility(url.toASCIIString())

    companion object {
        private const val URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json"
    }
}
