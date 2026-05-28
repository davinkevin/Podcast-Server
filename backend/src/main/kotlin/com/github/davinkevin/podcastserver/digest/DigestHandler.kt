package com.github.davinkevin.podcastserver.digest

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.item.ItemHAL
import com.github.davinkevin.podcastserver.item.toHAL
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.extension

private val DEFAULT_WITHIN: Duration = Duration.ofDays(7)
private const val DEFAULT_MAX_ITEMS_PER_PODCAST = 12

class DigestHandler(
    private val digestService: DigestService,
    private val clock: Clock,
) {

    fun find(r: ServerRequest): ServerResponse {
        val within = r.paramOrNull("within")?.let(Duration::parse) ?: DEFAULT_WITHIN
        val maxItemsPerPodcast = r.paramOrNull("maxItemsPerPodcast")?.toInt() ?: DEFAULT_MAX_ITEMS_PER_PODCAST

        val since = OffsetDateTime.now(clock).minus(within)

        val podcasts = digestService.findActivePodcastsSince(since, maxItemsPerPodcast)

        return ServerResponse.ok().body(podcasts.toHAL())
    }
}

data class DigestHAL(val content: List<DigestPodcastHAL>)

data class DigestPodcastHAL(
    val id: UUID,
    val title: String,
    val cover: CoverHAL,
    val itemCount: Int,
    val items: List<ItemHAL>,
) {
    data class CoverHAL(val id: UUID, val width: Int, val height: Int, val url: URI, val proxyURL: URI)
}

private fun List<DigestPodcast>.toHAL() = DigestHAL(content = map { it.toHAL() })

private fun DigestPodcast.toHAL(): DigestPodcastHAL {
    val coverProxyURL = UriComponentsBuilder.fromPath("/")
        .pathSegment("api", "v1", "podcasts", id.toString(), "cover." + cover.extension())
        .build(true)
        .toUri()

    return DigestPodcastHAL(
        id = id,
        title = title,
        cover = DigestPodcastHAL.CoverHAL(cover.id, cover.width, cover.height, cover.url, coverProxyURL),
        itemCount = itemCount,
        items = items.map { it.toHAL() },
    )
}

private fun Cover.extension(): String = Path(url.path).extension.ifBlank { "jpg" }
