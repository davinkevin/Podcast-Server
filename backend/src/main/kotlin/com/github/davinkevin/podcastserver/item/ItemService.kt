package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.storage.DeleteRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.io.path.Path

/**
 * Created by kevin on 2019-02-09
 */
class ItemService(
    private val repository: ItemRepository,
    private val podcastRepository: PodcastRepository,

    private val idm: ItemDownloadManager,
    private val file: FileStorageService,

    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(ItemService::class.java)!!

    fun deleteItemOlderThan(date: OffsetDateTime) {
        log.info("Deletion of items older than {}", date)
        val items = repository.findAllToDelete(date)

        items.forEach { file.delete(it) }

        repository.updateAsDeleted(items.map { it.id })
    }

    fun findById(id: UUID): Item? = repository.findById(id)

    fun reset(id: UUID): Item? {
        val isDownloading = idm.isInDownloadingQueueById(id)
        if (isDownloading) {
            return findById(id)
        }

        val canBeDeleted = repository.hasToBeDeleted(id)
        if (!canBeDeleted) {
            return findById(id)
        }

        val item = repository.findById(id)
            ?: return null

        if (item.isDownloaded() && item.fileName != Path("")) {
            file.delete(DeleteRequest.ForItem(item.id, item.fileName!!, item.podcast.title))
        }

        return repository.resetById(id)!!
    }

    fun search(
        q: String,
        tags: List<String>,
        status: List<Status>,
        page: ItemPageRequest,
        podcastId: UUID? = null
    ): PageItem {
        return repository.search(q = q, tags = tags, status = status, page = page, podcastId = podcastId)
    }

    fun upload(podcastId: UUID, uploadedFile: UploadedFile): Item {
        val podcast = podcastRepository.findById(podcastId)!!

        val filename = uploadedFile
            .filename
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            .let(Paths::get)

        val uploadRequest = UploadRequest.ForItemFromStream(
            podcastTitle = podcast.title,
            fileName = filename,
            content = uploadedFile.inputStream,
            length = uploadedFile.size
        )
        file.upload(uploadRequest)

        val metadata = file.metadata(podcast.title, filename)!!

        val (_, p2, p3) = uploadedFile.filename.split(" - ")
        val title = p3.substringBeforeLast(".")
        val date = LocalDate.parse(p2, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val time = LocalTime.of(0, 0)
        val pubDate = ZonedDateTime.of(date, time, ZoneId.systemDefault())

        val item = ItemForCreation(
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

        val createdItem = repository.create(item)!!
        podcastRepository.updateLastUpdate(podcastId)

        return createdItem
    }

    fun findPlaylistsContainingItem(itemId: UUID) = repository.findPlaylistsContainingItem(itemId)

    fun deleteById(itemId: UUID) {
        idm.removeItemFromQueueAndDownload(itemId)

        val item = repository.deleteById(itemId)

        if (item !== null) {
            file.delete(item)
        }
    }
}

private fun Cover.toCoverForCreation() = CoverForCreation(width, height, url)
