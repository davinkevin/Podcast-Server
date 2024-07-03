package com.github.davinkevin.podcastserver.update.updaters.dailymotion

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.utils.MatcherExtractor
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DailymotionUpdater(
    private val rc: RestClient,
    private val image: ImageService
): Updater {

    override fun signatureOf(url: URI): String? {
        val userName = USER_NAME_EXTRACTOR
            .on(url.toASCIIString())
            .group(1)
            ?: error("username not found")

        val channelDetails = rc
            .get()
            .uri("/user/{userName}/videos?fields=id", userName)
            .retrieve()
            .body<DailymotionResult>()
            ?: return null

        if (channelDetails.list.isEmpty()) {
            return ""
        }

        val sign = channelDetails.list
            .map { it.id }
            .sorted()
            .reduce { t, u -> """$t, $u""" }
            .let { DigestUtils.md5DigestAsHex(it.toByteArray()) }

        return sign
    }

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> {
        val userName = USER_NAME_EXTRACTOR.on(podcast.url.toASCIIString()).group(1) ?: error("username not found")

        val channelDetails = rc
            .get()
            .uri("/user/{userName}/videos?fields=created_time,description,id,thumbnail_720_url,title", userName)
            .retrieve()
            .body<DailymotionDetailsResult>()
            ?: return emptyList()

        return channelDetails.list.map {
            val cover = it.cover?.let(image::fetchCoverUpdateInformation)

            ItemFromUpdate(
                url = URI("https://www.dailymotion.com/video/${it.id}"),
                cover = cover,
                title = it.title,
                pubDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(it.creationDate!!), ZoneId.of("Europe/Paris")),
                description = it.description!!,
                mimeType = "video/mp4"
            )
        }
    }

    override fun type() = Type("Dailymotion", "Dailymotion")
    override fun compatibility(url: String): Int = when {
        "www.dailymotion.com" in url -> 1
        else -> Integer.MAX_VALUE
    }
}

// http://www.dailymotion.com/karimdebbache
private val USER_NAME_EXTRACTOR = MatcherExtractor.from("^.+dailymotion.com/(.*)")

private class DailymotionDetailsResult(val list: Set<DailymotionVideoDetail> = emptySet()) {
    class DailymotionVideoDetail(
        val id: String,
        val title: String,
        val description: String? = null,
        @JsonProperty("created_time") val creationDate: Long? = null,
        @JsonProperty("thumbnail_720_url") val cover: URI? = null
    )
}

private class DailymotionResult(val list: Set<DailymotionUpdaterVideoDetail> = emptySet()) {
    class DailymotionUpdaterVideoDetail(val id: String)
}
