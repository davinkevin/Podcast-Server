package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.getOrElse
import arrow.core.toOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.manager.worker.youtube.YoutubeByApiUpdater.Companion.URL_PAGE_BASE
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.service.properties.Api
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by kevin on 13/09/2018
 */
@Component
@ConditionalOnProperty(name = ["podcastserver.api.youtube"])
class YoutubeByApiUpdater(val htmlService: HtmlService, val jsonService: JsonService, val api: Api, val signatureService: SignatureService): Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun findItems(podcast: Podcast): Set<Item> {
        log.info("Youtube Update by API")

        val playlistId = findPlaylistId(podcast.url!!)
        val fetch = fetchWithCache()

        return generateSequence(fetch(asApiPlaylistUrl(playlistId))) { fetch(asApiPlaylistUrl(playlistId, it.nextPageToken)) }
                .takeUntil { it.nextPageToken.isNotEmpty() }
                .take(MAX_PAGE)
                .flatMap { it.items.asSequence() }
                .map { it.toItem() }
                .toSet()
    }

    override fun signatureOf(podcast: Podcast): String {
        val playlistId = findPlaylistId(podcast.url!!)

        val ids = jsonService.parseUrl(asApiPlaylistUrl(playlistId)).k()
                .map { JsonService.to(YoutubeApiResponse::class.java).apply(it) }
                .map { it.items }
                .getOrElse { listOf() }
                .joinToString { it.snippet.resourceId.videoId }

        return if(ids.isEmpty()) ""
        else signatureService.fromText(ids)
    }

    private fun findPlaylistId(url: String): String =
            if (isPlaylist(url)) playlistIdOf(url)
            else transformChannelIdToPlaylistId(channelIdOf(htmlService, url))

    private fun fetchWithCache() : (url: String) -> YoutubeApiResponse {
        val cache = hashMapOf<String, YoutubeApiResponse>()
        return { url -> cache.computeIfAbsent(url) { fetchPage(url) } }
    }

    private fun fetchPage(url: String): YoutubeApiResponse {
        return jsonService.parseUrl(url).k()
                .map(JsonService.to(YoutubeApiResponse::class.java)::apply)
                .getOrElse { YoutubeApiResponse(listOf()) }
    }

    private fun transformChannelIdToPlaylistId(channelId: String) =
            if (channelId.startsWith("UC")) channelId.replaceFirst("UC".toRegex(), "UU")
            else channelId

    private fun asApiPlaylistUrl(playlistId: String, pageToken: String = ""): String {
        val url = API_PLAYLIST_URL.format(playlistId, api.youtube)
        return if (pageToken.isEmpty()) url
        else "$url&pageToken=$pageToken"
    }

    override fun type() = _type()
    override fun compatibility(url: String?) = _compatibility(url)

    companion object {
        private const val MAX_PAGE = 10
        private const val API_PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s&key=%s"
        internal const val URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class YoutubeApiResponse(val items: List<YoutubeApiItem>, val nextPageToken: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class YoutubeApiItem(val snippet: Snippet) {
    fun toItem() = Item().apply {
        title = snippet.title
        description = snippet.description
        pubDate = snippet.pubDate()
        url = snippet.resourceId.url()
        cover = snippet.cover()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Snippet(val title: String, val resourceId: ResourceId, val description: String, val publishedAt: String, val thumbnails: Thumbnails = Thumbnails()) {
    fun pubDate() = ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)!!
    fun cover() = this.thumbnails
            .betterThumbnail()
            .map { Cover().apply { url = it.url; width = it.width; height = it.height } }
            .getOrElse { Cover.DEFAULT_COVER }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ResourceId(var videoId: String) {
    fun url() = URL_PAGE_BASE.format(videoId)
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Thumbnails(
        val maxres: Thumbnail? = null,
        val standard: Thumbnail? = null,
        val high: Thumbnail? = null,
        val medium: Thumbnail? = null,
        @field:JsonProperty("default") val byDefault: Thumbnail? = null
) {

    fun betterThumbnail(): arrow.core.Option<Thumbnail> =
            when {
                maxres != null -> maxres
                standard != null -> standard
                high != null -> high
                medium != null -> medium
                byDefault != null -> byDefault
                else -> null
            }.toOption()
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Thumbnail(var url: String? = null, var width: Int? = null, var height: Int? = null)

private fun <T> Sequence<T>.takeUntil(pred: (T) -> Boolean): Sequence<T> {
    var shouldContinue = true
    return takeWhile {
        val result = shouldContinue
        shouldContinue = pred(it)
        result
    }
}
