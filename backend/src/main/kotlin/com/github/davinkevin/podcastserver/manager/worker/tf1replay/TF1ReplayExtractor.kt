package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.M3U8Service
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.UrlService.Companion.USER_AGENT_DESKTOP
import com.github.davinkevin.podcastserver.service.UrlService.Companion.USER_AGENT_KEY
import com.github.davinkevin.podcastserver.service.UrlService.Companion.USER_AGENT_MOBILE
import com.github.davinkevin.podcastserver.utils.k
import com.jayway.jsonpath.TypeRef
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * Created by kevin on 12/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
class TF1ReplayExtractor(val htmlService: HtmlService, val jsonService: JsonService, val urlService: UrlService, val m3U8Service: M3U8Service) : Extractor {

    private val log = LoggerFactory.getLogger(TF1ReplayExtractor::class.java)!!

    // https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/quotidien-deuxieme-partie-21-juin-2017.html
    override fun getFileName(item: Item): String =
            item.url!!
                    .substringAfterLast("/")
                    .substringBeforeLast("?")
                    .baseName() + ".mp4"

    override fun extract(item: Item): DownloadingItem {

        val slug = item.url!!.substringAfterLast("/")
                .substringBeforeLast(".")

        val t = htmlService.get(item.url!!).k()
                .map { it.select("script") }
                .getOrElse { throw RuntimeException("no script tag found in the page") }
                .map { it.html() }
                .firstOption { it.contains("APOLLO_STATE") }
                .map { it.substringAfter("=").trim(';') }
                .flatMap { jsonService.parse(it).toOption() }

        val jsonContext = t
                .getOrElse { throw RuntimeException("error during extraction of json data from page") }

        val streamId = JsonService.to(TYPE_KEYS)
                .apply(jsonContext)
                .filter { it.value.containsKey("slug") }
                .filter { it.value.containsKey("streamId") }
                .filter { it.value["slug"] == slug }
                .map { it.value["streamId"].toString() }
                .first()

        val m3uUrl = getM3U8url(streamId)
        val highestQuality = getHighestQualityUrl(m3uUrl)

        return DownloadingItem(item, listOf(highestQuality), getFileName(item), USER_AGENT_MOBILE)
    }

    private fun getM3U8url(id: String): String {
        return Try {
            urlService.get("http://www.wat.tv/get/webhtml/$id")
                    .header(USER_AGENT_KEY, USER_AGENT_MOBILE)
                    .asString()
        }
                .map { it.body }
                .map { jsonService.parse(it) }
                .map { JsonService.to(TF1ReplayVideoUrlExtractorItem::class.java).apply(it) }
                .map { it.hls }
                .map { removeBitrate(it) }
                .getOrElse { "http://wat.tv/get/ipad/$id.m3u8" }
    }

    // http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001
    private fun removeBitrate(url: String): String =
            UrlService.removeQueryParameters(url) + "?" + url
                    .substringAfter( "?")
                    .split("&")
                    .map { toTuple(it) }
                    .filterNot { it.first.contains("bitrate") }
                    .joinToString("&") { "${it.first}=${it.second}" }

    private fun toTuple(params: String): Pair<String, String> {
        val split = params.split("=".toRegex(), 2)
        return Pair(split[0], split[1])
    }

    private fun getHighestQualityUrl(url: String): String {
        val body = try {
            urlService.get(url)
                    .header(USER_AGENT_KEY, USER_AGENT_MOBILE)
                    .asString()
                    .rawBody
        } catch (e: Exception) {
            throw RuntimeException("Request end up on error for $url", e)
        }

        val realUrl = urlService.getRealURL(url, Consumer { c -> c.setRequestProperty(USER_AGENT_KEY, USER_AGENT_DESKTOP) })

        return m3U8Service.findBestQuality(body)
                .map { urlService.addDomainIfRelative(realUrl, it) }
                .getOrElse { throw RuntimeException("No m3u8 url found in $url") }
    }

    override fun compatibility(url: String?) =
            if ("www.tf1.fr" in (url ?: "")) 1
            else Integer.MAX_VALUE

    companion object {
        private val TYPE_KEYS = object : TypeRef<Map<String, Map<String, Any>>>() {}
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TF1ReplayVideoUrlExtractorItem(val hls: String)

private fun String.baseName() = FilenameUtils.getBaseName(this)
