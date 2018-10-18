package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
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
class MyCanalExtractor(val htmlService: HtmlService, val jsonService: JsonService) : Extractor {

    override fun extract(item: Item) =
            htmlService.get(item.url).k()
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
                    .map { DownloadingItem(item, listOf(it.hls).toVΛVΓ(), getFileName(item), null) }
                    .getOrElse { throw RuntimeException("Error during extraction of ${item.title} at url ${item.url}") }

    override fun getFileName(item: Item) = "${super.getFileName(item)}.mp4"

    override fun compatibility(url: String?) = myCanalCompatibility(url)

    companion object {
        private const val URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json"
    }
}
