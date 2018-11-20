package com.github.davinkevin.podcastserver.manager.worker.sixplay

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.TypeRef
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.select.Elements
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by kevin on 20/12/2016 for Podcast Server
 */
@Component
class SixPlayUpdater(private val signatureService: SignatureService, private val htmlService: HtmlService, private val jsonService: JsonService, private val imageService: ImageService) : Updater {

    override fun getItems(podcast: Podcast) : io.vavr.collection.Set<Item> =
            htmlService.get(podcast.url)
                    .map { it.select("script") }
                    .map { extractItems(it) }
                    .getOrElse { setOf() }
                    .toVΛVΓ()


    private fun extractItems(script: Elements): Set<Item> {
        val root6Play = extractJson(script)
                .getOrElse { throw RuntimeException("No parsable JS found in the page") }

        val programId = JsonService.to(PROGRAM_ID_SELECTOR, TYPE_KEYS).apply(root6Play)
                .toOption()
                .map { it.keys }
                .flatMap { it.firstOption() }
                .map { it.toInt() }
                .getOrElse{ throw RuntimeException("programId not found in react store") }

        val programCode = JsonService.to(PROGRAM_CODE_SELECTOR.format(programId), String::class.java)
                .apply(root6Play)

        val basePath = URL_TEMPLATE_PODCAST
                .format(programCode, programId)

        return JsonService.to(VIDEO_BY_ID_SELECTOR, TYPE_ITEMS)
                .apply(root6Play)
                .map { convertToItem(it, basePath) }
                .toSet()
    }

    private fun convertToItem(i: SixPlayItem, basePath: String) =
            Item().apply {
                title =  i.title!!
                pubDate =  i.getLastDiffusion()
                length =  i.duration
                url =  i.url(basePath)
                description =  i.description
                cover =  i.cover().map { imageService.getCoverFromURL(it ) }.orNull()
            }

    private fun extractJson(elements: Elements): Option<DocumentContext> =
            elements
                    .toSet()
                    .firstOption { it.html().contains("root.") }
                    .map { it.html() }
                    .map { StringUtils.substringBetween(it, " = ", "}(this));") }
                    .map { it.removeJs() }
                    .map { jsonService.parse(it) }

    private fun String.removeJs() = this
            .trim { v -> v <= ' ' }
            .replace("function [^}]*".toRegex(), "{}")
            .removeSuffix(";")

    override fun signatureOf(podcast: Podcast) =
            htmlService.get(podcast.url).k()
                    .map { it.select("script") }
                    .flatMap { extractJson(it) }
                    .map { JsonService.extract<Any>("video.programVideosBySubCategory").apply(it) }
                    .map { signatureService.fromText(it.toString()) }
                    .getOrElse { throw RuntimeException("Error during signature of podcast " + podcast.title) }

    override fun type() = Type("SixPlay", "6Play")

    override fun compatibility(url: String?) = isFrom6Play(url)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class SixPlayItem(
            var display_image: Image? = null,
            var code: String? = null,
            var description: String? = null,
            var title: String? = null,
            var lastDiffusion: String? = null /* 2016-12-18 11:20:00 */,
            var duration: Long? = null,
            var id: String? = null
    ){

        fun getLastDiffusion(): ZonedDateTime {
            return Option.fromNullable(lastDiffusion)
                    .map { ZonedDateTime.of(LocalDateTime.parse(it, DATE_FORMATTER), ZoneId.of("Europe/Paris")) }
                    .getOrElse { ZonedDateTime.now() }
        }

        fun url(basePath: String): String = "$basePath$code${shortId()}"

        fun cover() =
                Option.fromNullable(display_image)
                        .map { it.url() }

        private fun shortId() =
                Option.fromNullable(id)
                        .map { it.substring(0, 1) }
                        .map { "-${it}_" }
                        .map { it + StringUtils.substringAfter(id, "_") }
                        .getOrElse { "" }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private class Image {

            var external_key: Int? = null

            fun url(): String {
                val path = path.format(external_key)
                return "$domain$path&hash=${DigestUtils.sha1Hex(path + salt)}"
            }

            companion object {
                private val domain = "https://images.6play.fr"
                private val path = "/v1/images/%s/raw?width=600&height=336&fit=max&quality=60&format=jpeg&interlace=1"
                private val salt = "54b55408a530954b553ff79e98"
            }
        }
    }

    companion object {

        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        private val TYPE_ITEMS = object : TypeRef<Set<SixPlayItem>>() {}
        private val TYPE_KEYS = object : TypeRef<Map<String, Any>>() {}

        private const val URL_TEMPLATE_PODCAST = "http://www.6play.fr/%s-p_%d/"
        private const val PROGRAM_CODE_SELECTOR = "program.programsById.%d.code"
        private const val PROGRAM_ID_SELECTOR = "program.programsById"
        private const val VIDEO_BY_ID_SELECTOR = "video.programVideoById[*]"

        fun isFrom6Play(url: String?) =
                if (url != null && url.contains("www.6play.fr/")) 1
                else Integer.MAX_VALUE
    }
}
