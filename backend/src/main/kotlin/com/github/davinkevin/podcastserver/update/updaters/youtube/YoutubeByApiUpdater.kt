package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.find.finders.meta
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByApiUpdater.Companion.URL_PAGE_BASE
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by kevin on 13/09/2018
 */
class YoutubeByApiUpdater(
    private val key: String,
    private val youtube: RestClient,
    private val googleApi: RestClient,
): Updater {

    private val log = LoggerFactory.getLogger(YoutubeByApiUpdater::class.java)

    override fun findItemsBlocking(podcast: PodcastToUpdate): List<ItemFromUpdate> {
        log.debug("find items of {}", podcast.url)

        val id = findPlaylistId(podcast.url)

        return generateSequence(fetchPageWithToken(id)) {
            if(it.nextPageToken.isNotEmpty()) {
                fetchPageWithToken(id, it.nextPageToken)
            } else null
        }
            .take(MAX_PAGE)
            .flatMap { it.items }
            .map(YoutubeApiItem::toItem)
            .toList()
    }

    override fun signatureOfBlocking(url: URI): String {
        log.debug("signature of {}", url)

        val id = findPlaylistId(url)

        val youtubeApiResponse = fetchPageWithToken(id)

        val elements = youtubeApiResponse.items
            .joinToString { i -> i.snippet.resourceId.videoId }

        if (elements.isEmpty()) {
            return ""
        }

        return DigestUtils.md5DigestAsHex(elements.toByteArray())
    }

    private fun fetchPageWithToken(id: String, pageToken: String = ""): YoutubeApiResponse {
        val response = kotlin.runCatching {
            googleApi
                .get()
                .uri {
                    it.path("/youtube/v3/playlistItems")
                        .queryParam("part", "snippet")
                        .queryParam("maxResults", 50)
                        .queryParam("playlistId", id)
                        .queryParam("key", key)
                        .apply {
                            if (pageToken.isNotEmpty()) {
                                this.queryParam("pageToken", pageToken)
                            }
                        }
                        .build()
                }
                .retrieve()
                .body<YoutubeApiResponse>()
        }

        return response.getOrNull() ?: return YoutubeApiResponse(emptyList())
    }


    private fun findPlaylistId(url: URI): String {
        val channelId = when {
            isPlaylist(url) -> findPlaylistIdFromPlaylistUrl(url)
            isHandle(url) -> findPlaylistIdUsingHtmlPage(url)
            isChannel(url) -> findChannelIdFromUrl(url)
            else -> findPlaylistIdFromUserName(url)
        } ?: error("channel id not found")

        return transformChannelIdToPlaylistId(channelId)
    }

    private fun findPlaylistIdFromPlaylistUrl(url: URI): String {
        return url.toASCIIString().substringAfter("list=")
    }

    private fun findPlaylistIdUsingHtmlPage(url: URI): String? {
        val page = youtube
            .get()
            .uri(url)
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(page, "https://www.youtube.com")

        return html.meta("itemprop=identifier")
    }

    private fun findChannelIdFromUrl(url: URI): String {
        return url.toASCIIString().substringAfterLast("/")
    }

    private fun findPlaylistIdFromUserName(url: URI): String? {
        val username = url.path.substringAfterLast("/")

        val channelDetails = googleApi
            .get()
            .uri {
                it.path("/youtube/v3/channels")
                    .queryParam("key", key)
                    .queryParam("forUsername", username)
                    .queryParam("part", "id")
                    .build()
            }
            .retrieve()
            .body<ChannelDetailsPage>()
            ?: return null

        return channelDetails.items.firstOrNull()?.id
    }

    private fun transformChannelIdToPlaylistId(channelId: String) =
        if (channelId.startsWith("UC")) channelId.replaceFirst("UC".toRegex(), "UU")
        else channelId

    override fun type() = type
    override fun compatibility(url: String): Int = youtubeCompatibility(url)

    companion object {
        private const val MAX_PAGE = 10
        internal const val URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class YoutubeApiResponse(val items: List<YoutubeApiItem>, val nextPageToken: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class YoutubeApiItem(val snippet: Snippet) {
    fun toItem() = ItemFromUpdate(
        title = snippet.title,
        description = snippet.description,
        pubDate = snippet.pubDate(),
        url = URI(snippet.resourceId.url()),
        cover = snippet.cover(),
        mimeType = "video/webm"
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Snippet(val title: String, val resourceId: ResourceId, val description: String, val publishedAt: String, val thumbnails: Thumbnails = Thumbnails()) {
    fun pubDate(): ZonedDateTime = ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)
    fun cover() = thumbnails
        .betterThumbnail()
        .map { ItemFromUpdate.Cover(url = URI(it.url!!), width = it.width!!, height = it.height!!) }
        .orNull()
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

    fun betterThumbnail(): Optional<Thumbnail> {
        val v = maxres ?: standard ?: high ?: medium ?: byDefault
        return Optional.ofNullable(v)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Thumbnail(var url: String? = null, var width: Int? = null, var height: Int? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ChannelDetailsPage(var items: List<ChannelDetails> = emptyList()) {
    internal data class ChannelDetails(var kind: String, var etag: String, var id: String)
}