package com.github.davinkevin.podcastserver.update.updaters.youtube

import arrow.core.toOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.manager.worker.CoverFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.ItemFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByApiUpdater.Companion.URL_PAGE_BASE
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by kevin on 13/09/2018
 */
class YoutubeByApiUpdater(
        private val key: String,
        private val youtubeClient: WebClient,
        private val googleApiClient: WebClient
): Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> = TODO("not required anymore...")
    override fun blockingSignatureOf(url: URI): String = TODO("not required anymore...")

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        log.debug("find items of {}", podcast.url)

        return findPlaylistId(podcast.url.toURL().file).flatMapMany { id ->
            Flux.range(1, MAX_PAGE)
                    .flatMap ({ pageNumber -> Mono
                            .subscriberContext()
                            .flatMap { c -> Mono
                                    .justOrEmpty(c.getOrEmpty<List<String>>("pageTokens"))
                                    .filter { it.isNotEmpty() }
                                    .map { it[pageNumber-2] }
                                    .flatMap { nextPageToken -> fetchPageWithToken(id, nextPageToken) }
                                    .switchIfEmpty { fetchPageWithToken(id) }
                                    .doOnNext {
                                        c.getOrEmpty<CopyOnWriteArrayList<String>>("pageTokens")
                                                .get()
                                                .add(it.nextPageToken)
                                    }
                            }
                    }, 1)
                    .takeUntil { it.nextPageToken.isEmpty() }
                    .flatMapIterable { it.items }
                    .map { it.toItem() }
                    .subscriberContext { c ->
                        log.debug("creation of cache")
                        c.put("pageTokens", CopyOnWriteArrayList<YoutubeApiResponse>())
                    }
        }
    }

    override fun signatureOf(url: URI): Mono<String> {
        log.debug("signature of {}", url)

        return findPlaylistId(url.toASCIIString()).flatMap { id ->
            fetchPageWithToken(id)
                    .map { it.items }
                    .map { items -> items.joinToString { it.snippet.resourceId.videoId } }
                    .filter { it.isNotEmpty() }
                    .map { DigestUtils.md5Hex(it) }
                    .switchIfEmpty { "".toMono() }
        }
    }

    private fun fetchPageWithToken(id: String, pageToken: String = "") =
            googleApiClient
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

    private fun findPlaylistId(path: String): Mono<String> {
        if (isPlaylist(path))
            return playlistIdOf(path).toMono()

        return youtubeClient
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, "https://www.youtube.com") }
                .flatMap { Mono.justOrEmpty(it.select("[data-channel-external-id]").firstOrNull()) }
                .map { it.attr("data-channel-external-id") }
                .map { transformChannelIdToPlaylistId(it) }
                .switchIfEmpty { RuntimeException("channel id not found").toMono() }
    }

    private fun transformChannelIdToPlaylistId(channelId: String) =
            if (channelId.startsWith("UC")) channelId.replaceFirst("UC".toRegex(), "UU")
            else channelId

    override fun type() = _type()
    override fun compatibility(url: String?) = _compatibility(url)

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
            cover = snippet.cover()
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Snippet(val title: String, val resourceId: ResourceId, val description: String, val publishedAt: String, val thumbnails: Thumbnails = Thumbnails()) {
    fun pubDate() = ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)!!
    fun cover() = this.thumbnails
            .betterThumbnail()
            .map { CoverFromUpdate (url = URI(it.url!!), width = it.width!!, height = it.height!! ) }
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
