package com.github.davinkevin.podcastserver.update

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.item.ItemForCreation
import com.github.davinkevin.podcastserver.item.ItemRepository
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import org.slf4j.LoggerFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime.now
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.time.measureTimedValue

class UpdateService(
    private val podcastRepository: PodcastRepository,
    private val itemRepository: ItemRepository,
    private val updaters: UpdaterSelector,
    private val liveUpdate: MessagingTemplate,
    private val fileService: FileStorageService,
    private val idm: ItemDownloadManager,
    private val updateExecutor: SimpleAsyncTaskExecutor,
) {

    private val log = LoggerFactory.getLogger(UpdateService::class.java)!!

    fun updateAll(force: Boolean, download: Boolean) {
        updateExecutor.execute { updateAllSync(force, download) }
    }

    private fun updateAllSync(force: Boolean, download: Boolean) {
        val (value, duration) = measureTimedValue {
            liveUpdate.isUpdating(true)

            val allRequests = podcastRepository.findAll()
                .collectList()
                .block()!!
                .filter { it.url != null }
                .map {
                    val signature = if (force || it.signature == null) UUID.randomUUID().toString() else it.signature
                    PodcastToUpdate(it.id, URI(it.url!!), signature)
                }

            val results = allRequests
                .map { Callable { updaters.of(it.url).update(it) } }
                .map { updateExecutor.submitCompletable(it).exceptionally { null } }
                .mapNotNull { it.get(5, TimeUnit.MINUTES) }

            results
                .forEach { (p, i, s) -> saveSignatureAndCreateItems(p, i, s) }

            liveUpdate.isUpdating(false)

            if (download) {
                idm.launchDownload().block()
            }

            return@measureTimedValue results
        }
        log.info("End of the global update with {} found, done in {}s", value.size, duration.inWholeSeconds)
    }

    fun update(podcastId: UUID) {
        liveUpdate.isUpdating(true)

        updateExecutor.execute {
            val podcast = podcastRepository.findById(podcastId).block()!!

            if (podcast.url == null) {
                return@execute
            }

            val request = PodcastToUpdate(podcast.id, URI(podcast.url), UUID.randomUUID().toString())

            val update = updaters.of(request.url).update(request)
                ?: return@execute

            saveSignatureAndCreateItems(update.podcast, update.items, update.newSignature)

            liveUpdate.isUpdating(false)
        }
    }

    private fun saveSignatureAndCreateItems(
        podcast: PodcastToUpdate,
        items: Set<ItemFromUpdate>,
        signature: String,
    ) {
        val realSignature = if (items.isEmpty()) "" else signature
        podcastRepository.updateSignature(podcast.id, realSignature).block()

        val creationRequests = items.map { it.toCreation(podcast.id)}
        val createdItems = itemRepository.create(creationRequests)

        if(createdItems.isEmpty()) {
            return
        }

        createdItems.forEach { item ->
            fileService.downloadItemCover(item).onErrorResume {
                log.error("Error during download of cover ${item.cover.url}")
                Mono.empty()
            }
                .block()
        }

        podcastRepository.updateLastUpdate(podcast.id).block()
    }
}

private fun ItemFromUpdate.toCreation(podcastId: UUID) = ItemForCreation(
    title = title!!,
    url = url.toASCIIString(),
    guid = this.guidOrUrl(),

    pubDate = pubDate?.toOffsetDateTime() ?: now(),
    downloadDate = null,
    creationDate = now(),

    description = description ?: "",
    mimeType = mimeType,
    length = length,
    fileName = null,
    status = Status.NOT_DOWNLOADED,

    podcastId = podcastId,
    cover = cover?.toCreation()
)

private fun ItemFromUpdate.Cover.toCreation() = CoverForCreation(width, height, url)

fun ImageService.fetchCoverUpdateInformationOrOption(url: URI?): Mono<Optional<ItemFromUpdate.Cover>> {
    return Mono.justOrEmpty(url)
        .flatMap { fetchCoverInformation(url!!) }
        .map { ItemFromUpdate.Cover(it.width, it.height, it.url) }
        .map { Optional.of(it) }
        .switchIfEmpty { Optional.empty<ItemFromUpdate.Cover>().toMono() }
}

fun ImageService.fetchCoverUpdateInformation(url: URI): ItemFromUpdate.Cover? {
    val coverInformation = fetchCoverInformation(url).block()
        ?: return null

    return ItemFromUpdate
        .Cover(coverInformation.width, coverInformation.height, coverInformation.url)
}
