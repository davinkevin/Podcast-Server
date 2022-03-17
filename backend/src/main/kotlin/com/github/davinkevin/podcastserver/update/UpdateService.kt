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
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime.now
import java.util.*

class UpdateService(
    private val podcastRepository: PodcastRepository,
    private val itemRepository: ItemRepository,
    private val updaters: UpdaterSelector,
    private val liveUpdate: MessagingTemplate,
    private val fileService: FileStorageService,
    private val parameters: PodcastServerParameters,
    private val idm: ItemDownloadManager
) {

    private val log = LoggerFactory.getLogger(UpdateService::class.java)!!

    fun updateAll(force: Boolean, download: Boolean): Mono<Void> {

        liveUpdate.isUpdating(true)

        podcastRepository
            .findAll()
            .parallel(parameters.maxUpdateParallels)
            .runOn(Schedulers.parallel())
            .filter { it.url != null }
            .map {
                val signature = if(force || it.signature == null) UUID.randomUUID().toString() else it.signature
                PodcastToUpdate(it.id, URI(it.url!!), signature)
            }
            .flatMap({ pu -> updaters.of(pu.url).update(pu) }, false, parameters.maxUpdateParallels)
            .flatMap { (p, i, s) -> saveSignatureAndCreateItems(p, i, s) }
            .sequential()
            .collectList()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext {
                liveUpdate.isUpdating(false)
                log.info("End of the global update with ${it.size} found")
            }
            .filter { download }
            .flatMap { idm.launchDownload() }
            .subscribe()

        return Mono.empty()
    }

    fun update(podcastId: UUID): Mono<Void> {
        liveUpdate.isUpdating(true)

        podcastRepository
            .findById(podcastId)
            .filter { it.url != null }
            .map { PodcastToUpdate(it.id, URI(it.url!!), UUID.randomUUID().toString()) }
            .flatMap { updaters.of(it.url).update(it) }
            .flatMap { (p, i, s) -> saveSignatureAndCreateItems(p, i, s) }
            .doOnTerminate { liveUpdate.isUpdating(false) }
            .subscribe()

        return Mono.empty()
    }

    private fun saveSignatureAndCreateItems(
        podcast: PodcastToUpdate,
        items: Set<ItemFromUpdate>,
        signature: String,
    ): Mono<Void> {
        val realSignature = if (items.isEmpty()) "" else signature
        val updateSignature = podcastRepository.updateSignature(podcast.id, realSignature).then(1.toMono())
        val createItems = items
            .map { it.toCreation(podcast.id)}
            .toMono()
            .flatMapMany { itemRepository.create(it) }
            .delayUntil { item -> fileService.downloadItemCover(item)
                .onErrorResume {
                    log.error("Error during download of cover ${item.cover.url}")
                    Mono.empty()
                }
            }
            .collectList()
            .filter { it.isNotEmpty() }
            .delayUntil { podcastRepository.updateLastUpdate(podcast.id) }

        return Mono.zip(updateSignature, createItems).then()
    }
}


private fun ItemFromUpdate.toCreation(podcastId: UUID) = ItemForCreation(
    title = title!!,
    url = url.toASCIIString(),

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
        .map { Optional.of<ItemFromUpdate.Cover>(it) }
        .switchIfEmpty { Optional.empty<ItemFromUpdate.Cover>().toMono() }
}
