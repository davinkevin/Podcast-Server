package com.github.davinkevin.podcastserver.update.updaters.gulli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.jsoup.Jsoup
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.ZonedDateTime

/**
 * Created by kevin on 14/03/2020
 */
class GulliUpdater(
    private val restClient: RestClient,
    private val image: ImageService,
    private val mapper: ObjectMapper
): Updater {

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> {
        val path = podcast.url
            .toASCIIString()
            .substringAfter("replay.gulli.fr")

        val page = restClient.get()
            .uri(path)
            .retrieve()
            .body<String>()
            ?: return emptyList()

        val html = Jsoup.parse(page, "https://replay.gulli.fr/")

        return html.select(".bloc_listing li a")
            .map { it.attr("href") }
            .mapNotNull(::findIndividualItem)
    }

    private fun findIndividualItem(url: String): ItemFromUpdate? {
        val page = restClient
            .get()
            .uri(url.substringAfter("replay.gulli.fr"))
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(page, "https://replay.gulli.fr/")
        val json = html.select("""script[type="application/ld+json"]""").html()

        val item = mapper.readValue<GulliItem>(json)
        val cover = image.fetchCoverUpdateInformation(item.thumbnailUrl)

        return ItemFromUpdate(
            title = item.name,
            description = item.description,
            pubDate = ZonedDateTime.parse(item.uploadDate),
            url = URI(url),
            cover = cover,
            mimeType = "video/mp4"
        )
    }

    override fun signatureOf(url: URI): String {
        val path = url.toASCIIString().substringAfter("replay.gulli.fr")

        val page = restClient.get()
            .uri(path)
            .retrieve()
            .body<String>()
            ?: return ""

        val html = Jsoup.parse(page, "https://replay.gulli.fr/")

        val itemUrl = html.select(".bloc_listing li a")
            .map { it.attr("href") }
            .ifEmpty { return "" }

        return itemUrl
            .toList()
            .sorted()
            .reduce { t, u -> "$t, $u" }
            .let(String::toByteArray)
            .let(DigestUtils::md5DigestAsHex)
    }

    override fun type() = Type("Gulli", "Gulli")
    override fun compatibility(url: String): Int = when {
        "replay.gulli.fr" in url -> 1
        else -> Integer.MAX_VALUE
    }
}

private data class GulliItem(
        val name: String,
        val description: String,
        val thumbnailUrl: URI,
        val uploadDate: String
)
