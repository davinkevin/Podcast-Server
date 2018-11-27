package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.collection.List
import io.vavr.collection.Set
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Created by kevin on 30/06/2017.
 */
@Component
class FranceTvUpdater(val signatureService: SignatureService, val htmlService: HtmlService, val imageService: ImageService, val jsonService: JsonService) : Updater {

    override fun getItems(podcast: Podcast): Set<Item> =
            htmlService
                    .get(podcast.url).k()
                    .map { it.select(LAST_VIDEOS_SELECTOR) }
                    .flatMap { it.firstOption() }
                    .map { it.select("li") }
                    .getOrElse { listOf<Element>() }
                    .map { htmlToItem(it) }
                    .toSet()
                    .toVΛVΓ()

    private fun htmlToItem(element: Element) =
            element.children()
                    .firstOption { it.tagName() == "a" }
                    .map { it.attr("data-video") }
                    .map { CATALOG_URL.format(it) }
                    .flatMap { jsonService.parseUrl(it).k() }
                    .map { JsonService.to(FranceTvItem::class.java).apply(it) }
                    .map { ftv ->
                        Item().apply {
                            title = ftv.title()
                            description = ftv.synopsis
                            pubDate = ftv.pubDate()
                            url = getUrl(element)
                            cover = imageService.getCoverFromURL(ftv.image)
                        }
                    }
                    .getOrElse { Item.DEFAULT_ITEM }

    private fun getUrl(element: Element) =
            element.children()
                    .first { it.tagName() == "a" }
                    .attr("href")
                    .addProtocolIfNecessary("https:")

    override fun signatureOf(podcast: Podcast): String {
        val listOfIds = htmlService.get(podcast.url).k()
                .map { p -> p.select(LAST_VIDEOS_SELECTOR) }
                .flatMap { it.firstOption() }
                .map { it.select("li") }
                .getOrElse { setOf<Element>() }
                .flatMap { it.children().firstOption { el -> el.tagName() == "a" }.toList() }
                .map { it.attr("data-video") }
                .toList()
                .sorted()
                .joinToString("-")

        return signatureService.fromText(listOfIds)
    }

    override fun type() = Type("FranceTv", "France•tv")

    override fun compatibility(url: String?) = isFromFranceTv(url)


    companion object {
        private val LAST_VIDEOS_SELECTOR = "#main ul.video-list"
        private val CATALOG_URL = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s"

        fun isFromFranceTv(url: String?) =
                if (StringUtils.contains(url, "www.france.tv")) 1
                else Integer.MAX_VALUE
    }
}

private fun String.addProtocolIfNecessary(protocol: String) =
        UrlService.addProtocolIfNecessary(protocol, this)

@JsonIgnoreProperties(ignoreUnknown = true)
private class FranceTvItem {

    var titre: String? = null
    var synopsis: String? = null
    var saison: String? = null
    var episode: String? = null
    var diffusion = Diffusion()
    var videos: List<Video> = List.empty()
    @JsonProperty("image_secure") var image: String? = null
    @JsonProperty("sous_titre") private val sousTitre: String? = null

    fun title(): String? {
        var title = titre

        if (!saison.isNullOrEmpty()) {
            title = "$title - S$saison"
        }

        if (!episode.isNullOrEmpty()) {
            title = "${title}E$episode"
        }

        if (!sousTitre.isNullOrEmpty()) {
            title = "$title - $sousTitre"
        }

        return title
    }

    fun pubDate(): ZonedDateTime =
            when {
                diffusion.timestamp == null -> ZonedDateTime.now()
                else -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(diffusion.timestamp!!), ZONE_ID)
            }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Diffusion {
        var timestamp: Long? = null
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Video {
        private val format: String? = null
        var url: String? = null
    }

    companion object {
        private val ZONE_ID = ZoneId.of("Europe/Paris")
    }
}
