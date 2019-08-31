package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.getOrElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.*
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.utils.k
import com.jayway.jsonpath.TypeRef
import io.vavr.collection.List
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Created by kevin on 30/06/2017.
 */
@Component
class FranceTvUpdater(
        val signatureService: SignatureService,
        val htmlService: HtmlService,
        val imageService: ImageService,
        val jsonService: JsonService
) : Updater {

    private val log = LoggerFactory.getLogger(FranceTvUpdater::class.java)!!

    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> {

        val urlBuilder = UriComponentsBuilder.fromHttpUrl(podcast.url.toASCIIString())

        return htmlService
                .get(toReplayUrl(podcast.url.toASCIIString())).k()
                .map { it.select("a[href]") }
                .getOrElse {
                    log.error("No items found for podcast ${podcast.id} at ${podcast.url}, the layout may have changed")
                    listOf<Element>()
                }
                .map { urlBuilder.replacePath(it.attr("href")).toUriString() }
                .map { urlToItem(it) }
                .toSet()
    }

    private fun toReplayUrl(url: String): String = "$url/replay-videos/ajax/?page=0"

    private fun urlToItem(itemUrl: String): ItemFromUpdate =
            htmlService
                    .get(itemUrl)
                    .map { it.select("script") }
                    .getOrElse {
                        log.error("No script found for item $itemUrl")
                        Elements()
                    }
                    .map { it.html() }
                    .firstOption { it.contains("FTVPlayerVideos") }
                    .map { it.substringAfter("=").trim(';') }
                    .flatMap { jsonService.parse(it).toOption() }
                    .map { JsonService.to(PAGE_ITEM).apply(it) }
                    .getOrElse { setOf() }
                    .firstOption { it.contentId in itemUrl }
                    .map { CATALOG_URL.format(it.videoId)}
                    .flatMap { jsonService.parseUrl(it).k() }
                    .map { JsonService.to(FranceTvItem::class.java).apply(it) }
                    .map { ftv -> ItemFromUpdate(
                        title = ftv.title()!!,
                        description = ftv.synopsis!!,
                        pubDate = ftv.pubDate(),
                        url = URI(itemUrl),
                        cover = imageService.fetchCoverInformation(ftv.image)?.toCoverFromUpdate()
                    ) }
                    .getOrElse { defaultItem }

    override fun blockingSignatureOf(url: URI): String {

        val listOfIds = htmlService
                .get(toReplayUrl(url.toASCIIString())).k()
                .map { it.select("a[href]") }
                .getOrElse {
                    log.error("No items found for podcast with url $url, the layout may have changed")
                    listOf<Element>()
                }
                .map { it.attr("href") }
                .sorted()
                .joinToString("-")

        return signatureService.fromText(listOfIds)
    }

    override fun type() = Type("FranceTv", "France•tv")

    override fun compatibility(url: String?) = isFromFranceTv(url)

    companion object {
        const val CATALOG_URL = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s"
        internal val PAGE_ITEM = object : TypeRef<Set<FranceTvPageItem>>() {}

        fun isFromFranceTv(url: String?) =
                if (StringUtils.contains(url, "www.france.tv")) 1
                else Integer.MAX_VALUE
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal class FranceTvPageItem(val contentId: String, val videoId: String)

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
