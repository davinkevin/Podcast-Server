package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.service.storage.CoverExistsRequest
import com.github.davinkevin.podcastserver.service.storage.ExternalUrlRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import org.springframework.web.servlet.function.paramOrNull
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

/**
 * Created by kevin on 2019-02-15
 */
class PodcastHandler(
    private val podcastService: PodcastService,
    private val fileService: FileStorageService
) {

    private var log = LoggerFactory.getLogger(PodcastHandler::class.java)

    fun findById(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("id").let(UUID::fromString)

        val podcast = podcastService.findById(id)
            ?: return ServerResponse.notFound().build()

        val body = podcast.toHAL()

        return ServerResponse.ok().body(body)
    }

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val podcasts = podcastService.findAll()
            .map(Podcast::toHAL)

        val body = FindAllPodcastHAL(podcasts)

        return ServerResponse.ok().body(body)
    }

    fun create(r: ServerRequest): ServerResponse {
        val creationRequest = r.body<PodcastCreationHAL>()
            .toPodcastCreation()

        val podcast = podcastService.save(creationRequest)

        return ServerResponse.ok().body(podcast.toHAL())
    }

    fun update(r: ServerRequest): ServerResponse {
        val updateRequest = r.body<PodcastUpdateHAL>()
            .toPodcastUpdate()

        val podcast = podcastService.update(updateRequest)

        return ServerResponse.ok().body(podcast.toHAL())
    }

    fun delete(r: ServerRequest): ServerResponse {
        val id = r.pathVariable("id").let(UUID::fromString)

        podcastService.deleteById(id)

        return ServerResponse.noContent().build()
    }

    fun cover(r: ServerRequest): ServerResponse {
        val host = r.extractHost()
        val id = r.pathVariable("id").let(UUID::fromString)

        val podcast = podcastService.findById(id)
            ?: return ServerResponse.notFound().build()

        log.debug("the url of the podcast cover is {}", podcast.cover.url)

        val coverExistsRequest = podcast.toCoverExistsRequest()
        val uri = when(val coverPath = fileService.coverExists(coverExistsRequest)) {
            is Path -> ExternalUrlRequest.ForPodcast(
                host = host,
                podcastTitle = podcast.title,
                file = coverPath,
            )
                .let(fileService::toExternalUrl)
            else -> podcast.cover.url
        }

        log.debug("Redirect cover to {}", uri)

        return ServerResponse.seeOther(uri).build()
    }

    fun findStatByPodcastIdAndPubDate(r: ServerRequest): ServerResponse = statsBy(r) { id, number -> podcastService.findStatByPodcastIdAndPubDate(id, number) }
    fun findStatByPodcastIdAndDownloadDate(r: ServerRequest): ServerResponse = statsBy(r) { id, number -> podcastService.findStatByPodcastIdAndDownloadDate(id, number) }
    fun findStatByPodcastIdAndCreationDate(r: ServerRequest): ServerResponse = statsBy(r) { id, number -> podcastService.findStatByPodcastIdAndCreationDate(id, number) }

    private fun statsBy(r: ServerRequest, proj: (id: UUID, n: Int) -> List<NumberOfItemByDateWrapper>): ServerResponse {
        val id = UUID.fromString(r.pathVariable("id"))
        val numberOfMonths = r.paramOrNull("numberOfMonths")?.toInt() ?: 1

        val stats = proj(id, numberOfMonths)

        return ServerResponse.ok().body(stats)
    }

    fun findStatByTypeAndCreationDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndCreationDate(number) }
    fun findStatByTypeAndPubDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndPubDate(number) }
    fun findStatByTypeAndDownloadDate(r: ServerRequest) = statsBy(r) { number -> podcastService.findStatByTypeAndDownloadDate(number) }
    private fun statsBy(r: ServerRequest, proj: (n: Int) -> List<StatsPodcastType>): ServerResponse {
        val numberOfMonths = r.paramOrNull("numberOfMonths")?.toInt() ?: 1

        val stats = proj(numberOfMonths)

        return ServerResponse.ok().body(StatsPodcastTypeWrapperHAL(stats))
    }
}

internal fun Podcast.toCoverExistsRequest() = CoverExistsRequest.ForPodcast(
    id = id,
    title = title,
    coverExtension = cover.extension()
)

@Suppress("unused")
private class FindAllPodcastHAL(val content: Collection<PodcastHAL>)
@Suppress("unused")
private class StatsPodcastTypeWrapperHAL(val content: Collection<StatsPodcastType>)

private data class PodcastHAL(val id: UUID,
                              val title: String,
                              val url: String?,
                              val hasToBeDeleted: Boolean,
                              val lastUpdate: OffsetDateTime?,
                              val type: String,

                              val tags: Collection<TagHAL>,

                              val cover: CoverHAL)

private data class CoverHAL(val id: UUID, val width: Int, val height: Int, val url: URI)
private data class TagHAL(val id: UUID, val name: String)

private fun Cover.extension(): String {
    return Path(url.path).extension.ifBlank { "jpg" }
}

private fun Podcast.toHAL(): PodcastHAL {
    val coverUrl = UriComponentsBuilder.fromPath("/")
        .pathSegment("api", "v1", "podcasts", this.id.toString(), "cover." + this.cover.extension())
        .build(true)
        .toUri()

    return PodcastHAL(
        id,
        title,
        url,
        hasToBeDeleted,
        lastUpdate,
        type,
        tags.map { TagHAL(it.id, it.name) },
        CoverHAL(cover.id, cover.width, cover.height, coverUrl)
    )
}

private data class PodcastCreationHAL(
    val title: String,
    val url: URI?,
    val tags: Collection<TagForCreationHAL>?,
    val type: String,
    val hasToBeDeleted: Boolean,
    val cover: CoverForCreationHAL
) {
    fun toPodcastCreation() = PodcastForCreation(
        title = title,
        url = url,
        tags = (tags ?: listOf()).map { TagForCreation(it.id, it.name) },
        type = type,
        hasToBeDeleted = hasToBeDeleted,
        cover = CoverForCreation(cover.width, cover.height, cover.url)
    )
}
private data class TagForCreationHAL(val id: UUID?, val name: String)
private data class CoverForCreationHAL(val width: Int, val height: Int, val url: URI)

private data class PodcastUpdateHAL(
    val id: UUID,
    val title: String,
    val url: URI?,
    val hasToBeDeleted: Boolean,
    val tags: Collection<TagForCreationHAL>?,
    val cover: CoverForCreationHAL
) {
    fun toPodcastUpdate() = PodcastForUpdate(
        id = id,
        title = title,
        url = url,
        hasToBeDeleted = hasToBeDeleted,
        tags = (tags ?: listOf()).map { TagForCreation(it.id, it.name) },
        cover = CoverForCreation(cover.width, cover.height, cover.url)
    )
}

