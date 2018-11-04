package com.github.davinkevin.podcastserver.manager.worker.sixplay

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.service.M3U8Service
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import com.jayway.jsonpath.TypeRef
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.service.JsonService
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by kevin on 26/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class SixPlayExtractor(val jsonService: JsonService, val m3U8Service: M3U8Service, val urlService: UrlService) : Extractor {

    override fun extract(item: Item): DownloadingItem {

        val m = URL_EXTRACTOR.on(item.url)

        val list = (m.group(1).k() to m.group(2).k())
                .map { toJsonUrl(it) }
                .flatMap { jsonService.parseUrl(it).k() }
                .map { it.read(ITEMS_EXTRACTOR, TYPE_ITEMS) }
                .getOrElse { throw RuntimeException("No element founds for ${item.id} at url ${item.url}") }

        val urls: List<String> = list
                .flatMap { keepBestQuality(it).toList() }
                .map { it.full_physical_path }
                .filterNotNull()

        return DownloadingItem(
                item,
                urls,
                getFileName(item),
                null
        )
    }

    private fun keepBestQuality(item: M6PlayItem): Option<M6PlayAssets> {
        val a = item.assets
                .filterNot { it.protocol == "primetime" }

        return a
                .firstOption{ (it.video_quality ?: "").contains("sd1") }.flatMap { transformSD1Ism(it) }
                .orElse { a.firstOption{ (it.video_quality ?: "").contains("sd3") }.flatMap { transformSd3Url(it) } }
                .orElse { a.firstOption { i -> "usp_hls_h264" == i.type } }
                .orElse { a.firstOption { i -> "hq" == i.video_quality } }
                .orElse { a.firstOption { i -> "hd" == i.video_quality } }
                .orElse { a.firstOption { i -> "sd" == i.video_quality } }
    }

    private fun transformSd3Url(asset: M6PlayAssets): Option<M6PlayAssets> = transformUrl(asset)
    private fun transformSD1Ism(asset: M6PlayAssets): Option<M6PlayAssets> = transformUrl(asset) { toFullQualityPlaylistUrl(it) }

    private fun transformUrl(asset: M6PlayAssets, urlMapper: (String) -> String = { it }): Option<M6PlayAssets> =
            urlService.getRealURL(asset.full_physical_path!!).toOption()
                    .map { urlMapper(it) }
                    .map { urlService.get(it)
                            .header(UrlService.USER_AGENT_KEY, UrlService.USER_AGENT_MOBILE)
                            .asString() }
                    .map { it.rawBody }
                    .flatMap { m3U8Service.findBestQuality(it).k() }
                    .map { urlService.addDomainIfRelative(urlService.getRealURL(asset.full_physical_path!!), it) }
                    .map { M6PlayAssets(asset.video_quality, it, asset.type, asset.protocol) }

    override fun getFileName(item: Item) = super.getFileName(item) + ".mp4"
    override fun compatibility(url: String?) = SixPlayUpdater.isFrom6Play(url)

    private fun toFullQualityPlaylistUrl(t: String): String {
        val base = t.substringBeforeLast("/")
        val name = base.substringAfterLast("/").replace(".ism", ".m3u8")
        return "$base/$name"
    }

    companion object {

        private const val ITEMS_EXTRACTOR = "clips[*]"
        private const val INFO_URL = "https://pc.middleware.6play.fr/6play/v2/platforms/m6group_web/services/6play/videos/%s_%s?with=clips&csa=5"
        private val TYPE_ITEMS = object : TypeRef<List<M6PlayItem>>(){}
        private val URL_EXTRACTOR = from("^.+6play\\.fr/.+-([a-z])_([0-9]*)")

        private fun expandType(type: String): String = when(type) {
            "p" -> "playlist"
            "c" -> "clip"
            else -> throw RuntimeException("Invalid type \"$type\" for 6play item")
        }

        private fun toJsonUrl(p: Pair<String, String>): String {
            return String.format(INFO_URL, expandType(p.first), p.second)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class M6PlayItem {
    var assets: List<M6PlayAssets> = listOf()
}

@JsonIgnoreProperties(ignoreUnknown = true)
private class M6PlayAssets(
        var video_quality: String? = null,
        var full_physical_path: String? = null,
        var type: String? = null,
        var protocol: String? = null
)

private infix fun <T, U> Option<T>.to(other: Option<U>): Option<Pair<T, U>> = this.flatMap { first -> other.map { first to it } }
