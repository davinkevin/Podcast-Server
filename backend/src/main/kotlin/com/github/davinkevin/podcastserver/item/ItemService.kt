package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.podcast.PodcastService
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.util.function.component1
import reactor.util.function.component2
import reactor.util.function.component3
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import com.github.davinkevin.podcastserver.cover.CoverRepositoryV2 as CoverRepository
import com.github.davinkevin.podcastserver.item.ItemRepositoryV2 as ItemRepository

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemService(
        private val repository: ItemRepository,
        private val idm: ItemDownloadManager,
        private val p: PodcastServerParameters,
        private val fileService: FileService,
        private val podcastService: PodcastService,
        private val coverRepository: CoverRepository,
        private val mimeTypeService: MimeTypeService
) {

    private val log = LoggerFactory.getLogger(ItemService::class.java)!!

    fun deleteOldEpisodes() = repository.
            findAllToDelete( p.limitDownloadDate().toOffsetDateTime() )
            .doOnSubscribe { log.info("Deletion of old items") }
            .delayUntil { fileService.deleteItem(it) }
            .collectList()
            .flatMap { repository.updateAsDeleted(it.map { v -> v.id }) }

    fun findById(id: UUID) = repository.findById(id)

    fun reset(id: UUID): Mono<Item> = deleteItemFiles(id)
            .then(repository.resetById(id))

    private fun deleteItemFiles(id: UUID): Mono<Void> = id.toMono()
            .filter { !idm.isInDownloadingQueueById(it) }
            .filterWhen { repository.hasToBeDeleted(it) }
            .flatMap { repository.findById(it) }
            .delayUntil { item -> item.toMono()
                    .filter { it.isDownloaded() }
                    .filter { !it.fileName.isNullOrEmpty() }
                    .map { DeleteItemInformation(it.id, it.fileName!!, it.podcast.title) }
                    .flatMap { fileService.deleteItem(it) }
            }
            .then()

    fun search(q: String?, tags: List<String>, statuses: List<Status>, page: ItemPageRequest, podcastId: UUID? = null): Mono<PageItem> =
            repository.search(q = q, tags = tags, statuses = statuses, page = page, podcastId = podcastId)

    fun upload(podcastId: UUID, file: FilePart): Mono<Item> {
        val filename = file.filename().replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return podcastService
                .findById(podcastId)
                .delayUntil { fileService.upload(p.rootfolder.resolve(it.title).resolve(filename), file) }
                .flatMap { podcast -> Mono.zip(
                        fileService.size(p.rootfolder.resolve(podcast.title).resolve(filename)),
                        coverRepository.save(CoverForCreation(podcast.cover.width, podcast.cover.height, podcast.cover.url)),
                        podcast.toMono()
                ) }
                .map { (length, cover, podcast) ->
                    val (_, p2, p3) = file.filename().split(" - ")
                    val title = p3.substringBeforeLast(".")
                    val pubDate = ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(p2, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.of(0, 0)), ZoneId.systemDefault())

                    ItemForCreation(
                            title = title,
                            url = null,

                            pubDate = pubDate.toOffsetDateTime(),
                            downloadDate = OffsetDateTime.now(),
                            creationDate = OffsetDateTime.now(),

                            description = podcast.description,
                            mimeType = mimeTypeService.probeContentType(p.rootfolder.resolve(podcast.title).resolve(file.filename())),
                            length = length,
                            fileName = filename,
                            status = Status.FINISH,

                            podcastId = podcast.id,
                            coverId = cover.id
                    )
                }
                .flatMap { repository.create(it) }
    }
}
