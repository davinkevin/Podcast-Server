package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.nio.file.Paths
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path

/**
 * Created by kevin on 2019-02-09
 */
class ItemService(
    private val repository: ItemRepository,
    private val podcastRepository: PodcastRepository,

    private val idm: ItemDownloadManager,
    private val p: PodcastServerParameters,

    private val file: FileStorageService,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(ItemService::class.java)!!

    fun deleteItemOlderThan(date: OffsetDateTime) = repository.
    findAllToDelete(date)
        .doOnSubscribe { log.info("Deletion of items older than {}", date) }
        .delayUntil { file.deleteItem(it) }
        .map { v -> v.id }
        .collectList()
        .flatMap { repository.updateAsDeleted(it) }

    fun findById(id: UUID) = repository.findById(id)

    fun reset(id: UUID): Mono<Item> = deleteItemFiles(id)
        .then(repository.resetById(id))

    private fun deleteItemFiles(id: UUID): Mono<Void> = id.toMono()
        .filterWhen { idm.isInDownloadingQueueById(it).map { v -> !v } }
        .filterWhen { repository.hasToBeDeleted(it) }
        .flatMap { repository.findById(it) }
        .delayUntil { item -> item.toMono()
            .filter { it.isDownloaded() }
            .filter { it.fileName != Path("") }
            .map { DeleteItemRequest(it.id, it.fileName!!, it.podcast.title) }
            .flatMap { file.deleteItem(it) }
        }
        .then()

    fun search(q: String, tags: List<String>, status: List<Status>, page: ItemPageRequest, podcastId: UUID? = null): Mono<PageItem> =
        repository.search(q = q, tags = tags, status = status, page = page, podcastId = podcastId)

    fun upload(podcastId: UUID, filePart: FilePart): Mono<Item> {
        val filename = Paths.get(filePart.filename().replace("[^a-zA-Z0-9.-]".toRegex(), "_"))

        return Mono.zip(
            file.cache(filePart, filename),
            podcastRepository.findById(podcastId)
        )
            .flatMap { (cacheFile, podcast) -> file.upload(podcast.title, cacheFile).thenReturn(podcast) }
            .flatMap { Mono.zip(
                file.metadata(it.title, filename),
                it.toMono()
            ) }
            .map { (metadata, podcast) ->
                val (_, p2, p3) = filePart.filename().split(" - ")
                val title = p3.substringBeforeLast(".")
                val date = LocalDate.parse(p2, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val time = LocalTime.of(0, 0)
                val pubDate = ZonedDateTime.of(date, time, ZoneId.systemDefault())

                ItemForCreation(
                    title = title,
                    url = null,
                    guid = filename.fileName.toString(),

                    pubDate = pubDate.toOffsetDateTime(),
                    downloadDate = OffsetDateTime.now(clock),
                    creationDate = OffsetDateTime.now(clock),

                    description = podcast.description,
                    mimeType = metadata.contentType,
                    length = metadata.size,
                    fileName = filename.fileName,
                    status = Status.FINISH,

                    podcastId = podcast.id,
                    cover = podcast.cover.toCoverForCreation()
                )
            }
            .flatMap { repository.create(it) }
            .delayUntil { podcastRepository.updateLastUpdate(podcastId) }
    }

    fun findPlaylistsContainingItem(itemId: UUID): Flux<ItemPlaylist> =
        repository.findPlaylistsContainingItem(itemId)

    fun deleteById(itemId: UUID): Mono<Void> {
        return idm.removeItemFromQueueAndDownload(itemId).then(
            repository
                .deleteById(itemId)
                .delayUntil { file.deleteItem(it) }
                .then()
        )
    }
}

private fun Cover.toCoverForCreation() = CoverForCreation(width, height, url)
