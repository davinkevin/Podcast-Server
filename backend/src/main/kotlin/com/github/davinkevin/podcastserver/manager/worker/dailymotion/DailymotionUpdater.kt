package com.github.davinkevin.podcastserver.manager.worker.dailymotion

import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.jayway.jsonpath.TypeRef
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.service.JsonService
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Component
class DailymotionUpdater(val signatureService: SignatureService, val jsonService: JsonService, val imageService: ImageService) : Updater {

    override fun getItems(podcast: Podcast): io.vavr.collection.Set<Item> {
        return USER_NAME_EXTRACTOR.on(podcast.url).group(1).k()
                .map { API_LIST_OF_ITEMS.format(it) }
                .flatMap { jsonService.parseUrl(it).k() }
                .map { it.read("list", LIST_DAILYMOTION_VIDEO_DETAIL_TYPE) }
                .getOrElse { setOf() }
                .map { Item().apply {
                    url = ITEM_URL.format(it.id)
                    cover = imageService.getCoverFromURL(it.cover)
                    title = it.title
                    pubDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(it.creationDate!!), ZoneId.of("Europe/Paris"))
                    description = it.description
                    }
                }
                .toSet()
                .toVΛVΓ()
    }

    override fun signatureOf(podcast: Podcast): String {
        return USER_NAME_EXTRACTOR.on(podcast.url).group(1).k()
                .map { API_LIST_OF_ITEMS.format(it) }
                .map { signatureService.fromUrl(it) }
                .getOrElse { throw RuntimeException("Username not Found") }
    }

    override fun type() = Type("Dailymotion", "Dailymotion")

    override fun compatibility(url: String?) =
            if ("www.dailymotion.com" in (url ?: "")) 1
            else Integer.MAX_VALUE

    companion object {
        const val API_LIST_OF_ITEMS = "https://api.dailymotion.com/user/%s/videos?fields=created_time,description,id,thumbnail_720_url,title"
        // http://www.dailymotion.com/karimdebbache
        private val USER_NAME_EXTRACTOR = from("^.+dailymotion.com/(.*)")
        private const val ITEM_URL = "http://www.dailymotion.com/video/%s"
        private val LIST_DAILYMOTION_VIDEO_DETAIL_TYPE = object : TypeRef<Set<DailymotionUpdaterVideoDetail>>(){}
    }
}

private class DailymotionUpdaterVideoDetail(
        val id: String? = null,
        val title: String? = null,
        val description: String? = null,
        @JsonProperty("created_time") val creationDate: Long? = null,
        @JsonProperty("thumbnail_720_url") val cover: String? = null
)