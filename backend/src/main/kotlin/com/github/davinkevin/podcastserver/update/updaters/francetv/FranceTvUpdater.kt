package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import io.micrometer.core.instrument.MeterRegistry
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FranceTvUpdater(
    private val franceTvClient: RestClient,
    private val image: ImageService,
    private val clock: Clock,
    override val registry: MeterRegistry,
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
            .select("main[role=main] .group")
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
        val cover = html.select("meta[property=og:image]").attr("content")
            .let(URI::create)
            .let(image::fetchCoverUpdateInformation)

        return ItemFromUpdate(
            title = html.select("meta[property=og:title]").attr("content"),
            description = html.select("meta[property=og:description]").attr("content"),
            pubDate = extractPubDate(html),
            url = URI("https://www.france.tv$pathUrl"),
            cover = cover,
            mimeType = "video/mp4"
        )
    }

    private fun extractPubDate(html: Document): ZonedDateTime? = runCatching {
        val pubDateBlock = html.select("#about-section [role=heading]").text()
        val blocks = pubDateBlock
            .substringBefore(" - ")
            .split(" ")
        val dateAsText = blocks[2]
        val timeAsText = blocks[4].replace("h", ":")
        val date = LocalDateTime.parse("$dateAsText $timeAsText", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

        ZonedDateTime.of(date, ZoneId.of("Europe/Paris"))
    }
        .getOrElse { ZonedDateTime.now(clock) }

    override fun signatureOf(url: URI): String {
        val replay = replayUrl(url)

        val page = franceTvClient
            .get()
            .uri(replay)
            .retrieve()
            .body<String>()
            ?: return ""

        val html = Jsoup.parse(page, url.toASCIIString())
        val ids = html.select("main[role=main] .group")
            .toList()
            .filter { !it.html().contains("indisponible") }
            .map { it.select("a[href]").attr("href") }
            .sorted()

        if (ids.isEmpty()) return ""

        return ids.reduce { t, u -> """$t-$u""" }
            .toByteArray()
            .let (DigestUtils::md5DigestAsHex)
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