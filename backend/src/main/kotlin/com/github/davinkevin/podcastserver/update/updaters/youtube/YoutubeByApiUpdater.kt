package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByApiUpdater.Companion.URL_PAGE_BASE
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by kevin on 13/09/2018
 */
class YoutubeByApiUpdater(
    private val key: String,
    private val youtube: WebClient,
    private val googleApi: WebClient,
): Updater {

    private val log = LoggerFactory.getLogger(YoutubeByApiUpdater::class.java)

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        log.debug("find items of {}", podcast.url)

        return findPlaylistId(podcast.url)
            .flatMapMany { id -> fetchPageWithToken(id)
                .expand { fetchPageWithToken(id, it.nextPageToken) }
            }
            .takeUntil { it.nextPageToken.isEmpty() }
            .take(MAX_PAGE)
            .flatMapIterable { it.items }
            .map { it.toItem() }
    }

    override fun signatureOf(url: URI): Mono<String> {
        log.debug("signature of {}", url)

        return findPlaylistId(url)
            .flatMap { id -> fetchPageWithToken(id) }
            .map { it.items.joinToString { i -> i.snippet.resourceId.videoId } }
            .filter { it.isNotEmpty() }
            .map { DigestUtils.md5DigestAsHex(it.toByteArray()) }
            .switchIfEmpty { "".toMono() }
    }

    private fun fetchPageWithToken(id: String, pageToken: String = "") =
        googleApi
            .get()
            .uri { it.path("/youtube/v3/playlistItems")
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
            .bodyToMono<YoutubeApiResponse>()
            .onErrorResume { YoutubeApiResponse(emptyList()).toMono() }

    private fun findPlaylistId(url: URI): Mono<String> {
        val channelId = when {
            isPlaylist(url) -> findPlaylistIdFromPlaylistUrl(url)
            isHandle(url) -> findPlaylistIdUsingHtmlPage(url)
            else -> findPlaylistIdFromUserName(url)
        }

        return channelId
            .map(::transformChannelIdToPlaylistId)
            .switchIfEmpty { RuntimeException("channel id not found").toMono() }
    }

    private fun findPlaylistIdUsingHtmlPage(url: URI): Mono<String> {
        return youtube
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono<String>()
            .map { Jsoup.parse(it, "https://www.youtube.com") }
            .flatMap { it.select("meta[itemprop=identifier]").firstOrNull().toMono() }
            .map { it.attr("content") }
    }

    private fun findPlaylistIdFromPlaylistUrl(url: URI): Mono<String> {
        return url.toASCIIString().substringAfter("list=").toMono()
    }

    private fun findPlaylistIdFromUserName(url: URI): Mono<String> {
        val username = url.path.substringAfterLast("/")

        return googleApi
            .get()
            .uri {
                it.path("/youtube/v3/channels")
                    .queryParam("key", key)
                    .queryParam("forUsername", username)
                    .queryParam("part", "id")
                    .build()
            }
            .retrieve()
            .bodyToMono<ChannelDetailsPage>()
            .flatMap { it.items.firstOrNull()?.id.toMono() }

    }

    private fun transformChannelIdToPlaylistId(channelId: String) =
        if (channelId.startsWith("UC")) channelId.replaceFirst("UC".toRegex(), "UU")
        else channelId

    override fun type() = type
    override fun compatibility(url: String): Int = youtubeCompatibility(url)

    companion object {
        private const val MAX_PAGE = 10L
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
