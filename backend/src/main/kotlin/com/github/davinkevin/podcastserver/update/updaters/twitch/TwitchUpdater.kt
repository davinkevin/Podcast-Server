package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Updater
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.HttpHeaders
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TwitchUpdater(
    private val rc: RestClient,
    private val token: TwitchTokenProvider,
    private val image: ImageService,
    private val videoTypes: List<String?>,
    override val registry: MeterRegistry,
) : Updater {

    override fun signatureOf(url: URI): String {
        val videos = fetchVideos(userIdOf(url), maxPages = 1)

        if (videos.isEmpty()) {
            return ""
        }

        return videos
            .map { it.id }
            .sorted()
            .joinToString(", ")
            .let { DigestUtils.md5DigestAsHex(it.toByteArray()) }
    }

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> =
        fetchVideos(userIdOf(podcast.url), maxPages = MAX_PAGE)
            .map { video ->
                ItemFromUpdate(
                    title = video.title,
                    description = video.description,
                    pubDate = video.publicationDate(),
                    url = URI(video.url),
                    cover = video.coverUrl()?.let(image::fetchCoverUpdateInformation),
                    mimeType = "video/mp4",
                )
            }

    /**
     * The only path to video data, so nothing can forget the in-progress exclusion.
     *
     * It matters on both sides: applied to [findItems] alone, the signature would flip
     * on every check while a broadcast is live and trigger a full update for nothing.
     * Here a vod enters the set only once the broadcast has finalised — exactly when
     * the signature should change and the item should appear.
     */
    private fun fetchVideos(userId: String, maxPages: Int): List<TwitchVideo> = videoTypes
        .flatMap { videoType -> fetchVideosOfType(userId, videoType, maxPages) }
        .filterNot { it.isInProgress() }
        .sortedByDescending { it.publicationDate() ?: EPOCH }

    private fun fetchVideosOfType(userId: String, videoType: String?, maxPages: Int): List<TwitchVideo> =
        generateSequence(fetchPage(userId, videoType, cursor = "")) { page ->
            page.pagination.cursor
                .takeIf { it.isNotEmpty() }
                ?.let { fetchPage(userId, videoType, cursor = it) }
        }
            .take(maxPages)
            .flatMap { it.data }
            .toList()

    private fun fetchPage(userId: String, videoType: String?, cursor: String): TwitchVideosResponse = token
        .withRetryOn401 { accessToken ->
            rc.get()
                .uri {
                    it.path("/helix/videos")
                        .queryParam("user_id", userId)
                        .queryParam("first", 100)
                        .queryParam("sort", "time")

                    if (videoType != null) it.queryParam("type", videoType)
                    if (cursor.isNotEmpty()) it.queryParam("after", cursor)

                    it.build()
                }
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body<TwitchVideosResponse>()
        }
        ?: TwitchVideosResponse()

    private fun userIdOf(url: URI): String {
        val login = extractLogin(url.toASCIIString())
            ?: error("login not found in $url")

        return rc.helixUser(token, login)?.id
            ?: error("user not found")
    }

    override fun type() = type
    override fun compatibility(url: String): Int = twitchCompatibility(url)

    companion object {
        private const val MAX_PAGE = 10
        private val EPOCH = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TwitchVideosResponse(
    val data: List<TwitchVideo> = emptyList(),
    val pagination: TwitchPagination = TwitchPagination(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TwitchPagination(val cursor: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TwitchVideo(
    val id: String,
    val url: String,
    val title: String = "",
    val description: String = "",
    @param:JsonProperty("published_at") val publishedAt: String = "",
    @param:JsonProperty("created_at") val createdAt: String = "",
    @param:JsonProperty("thumbnail_url") val thumbnailUrl: String = "",
) {

    /**
     * A live broadcast shows up in `/helix/videos` right away, as an `archive` with a
     * growing duration and no usable thumbnail yet. Turning it into an item would have
     * yt-dlp download the *stream* and never stop.
     */
    fun isInProgress(): Boolean = thumbnailUrl.isBlank() || THUMBNAIL_BEING_PROCESSED in thumbnailUrl

    fun publicationDate(): ZonedDateTime? = publishedAt
        .ifBlank { createdAt }
        .takeIf { it.isNotBlank() }
        ?.let(ZonedDateTime::parse)

    /** the raw url carries `%{width}`/`%{height}` placeholders and is invalid as is */
    fun coverUrl(): URI? = thumbnailUrl
        .takeIf { it.isNotBlank() }
        ?.replace("%{width}", "1280")
        ?.replace("%{height}", "720")
        ?.let(::URI)

    private companion object {
        private const val THUMBNAIL_BEING_PROCESSED = "404_processing"
    }
}
