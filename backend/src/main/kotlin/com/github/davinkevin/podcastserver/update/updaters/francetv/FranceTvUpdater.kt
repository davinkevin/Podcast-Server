package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.DurationUnit

class FranceTvUpdater(
    private val franceTvClient: RestClient,
    private val image: ImageService,
    private val mapper: ObjectMapper,
    private val clock: Clock
): Updater {

    private val log = LoggerFactory.getLogger(FranceTvUpdater::class.java)

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> {

        val url = podcast.url.toASCIIString()

        log.debug("Fetch $url")

        val replay = replayUrl(podcast.url)

        val page = franceTvClient
            .get()
            .uri(replay)
            .retrieve()
            .body<String>()
            ?: return emptyList()

        val html = Jsoup.parse(page, url)

        val urls = html
            .select(".c-wall__item > [data-video-id]")
            .toList()
            .filter { !it.html().contains("indisponible") }
            .map { it.select("a[href]").attr("href") }

        return urls
            .mapNotNull(::urlToItem)
    }

    private fun urlToItem(pathUrl: String): ItemFromUpdate? {

        val page = franceTvClient
            .get()
            .uri(pathUrl)
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(page, "https://www.france.tv/")

        val jsonldTag = html.select("script[type=application/ld+json]").firstOrNull()
            ?: error("""No <script type="application/ld+json"></script> found""")

        val jsonLd = mapper.readTree(jsonldTag.html())
        val videoObject = jsonLd.firstOrNull { it.get("@type").asText() == "VideoObject" }
            ?: error("""No element of type VideoObject""")

        val pubDate = when(val uploadDate = videoObject["uploadDate"]?.asText()) {
            null -> ZonedDateTime.now(clock)
            else -> ZonedDateTime.parse(uploadDate, DateTimeFormatter.ISO_DATE_TIME)
        }

        val cover = videoObject.get("thumbnailUrl")
            .asSequence()
            .map(JsonNode::asText)
            .map(URI::create)
            .map(image::fetchCoverUpdateInformation)
            .firstOrNull()

        return ItemFromUpdate(
            title = videoObject.get("name").asText().replaceFirst("Secrets d'Histoire ", ""),
            description = videoObject.get("description").asText(),
            length = Duration.parse(videoObject.get("duration").asText()).toLong(DurationUnit.SECONDS),
            pubDate = pubDate,
            url = URI("https://www.france.tv$pathUrl"),
            cover = cover,
            mimeType = "video/mp4"
        )
    }

    override fun signatureOf(url: URI): String {
        val replay = replayUrl(url)

        val page = franceTvClient
            .get()
            .uri(replay)
            .retrieve()
            .body<String>()
            ?: return ""

        val html = Jsoup.parse(page, url.toASCIIString())
        val ids = html.select(".c-wall__item > [data-video-id]")
            .toList()
            .filter { !it.html().contains("indisponible") }

        if (ids.isEmpty()) return ""

        return ids
            .asSequence()
            .map { it.select("a[href]").attr("href") }
            .sorted()
            .reduce { t, u -> """$t-$u""" }
            .let { DigestUtils.md5DigestAsHex(it.toByteArray()) }
    }

    override fun type() = Type("FranceTv", "France•tv")

    override fun compatibility(url: String): Int = when {
        "www.france.tv" in url -> 1
        else -> Integer.MAX_VALUE
    }

    private fun replayUrl(url: URI): String {
        return "${url.toASCIIString()}/toutes-les-videos/"
            .substringAfter("https://www.france.tv/")
    }

}