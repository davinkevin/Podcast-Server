package com.github.davinkevin.podcastserver.update

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.item.ItemForCreation
import com.github.davinkevin.podcastserver.item.ItemRepository
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.update.updaters.CoverFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.time.OffsetDateTime.now
import java.util.*

class UpdateService(
        private val podcastRepository: PodcastRepository,
        private val itemRepository: ItemRepository,
        private val updaters: UpdaterSelector,
        private val liveUpdate: MessagingTemplate,
        private val fileService: FileService,
        private val parameters: PodcastServerParameters,
        private val idm: ItemDownloadManager
) {

    val log = LoggerFactory.getLogger(UpdateService::class.java)!!

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
                .flatMap {pu -> updaters.of(pu.url).update(pu) }
                .flatMap { (p, i, s) -> saveSignatureAndCreateItems(p, i, s) }
                .sequential()
                .doOnTerminate { liveUpdate.isUpdating(false).also { log.info("End of the global update") } }
                .doOnTerminate { if (download) idm.launchDownload() }
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

    private fun saveSignatureAndCreateItems(podcast: PodcastToUpdate, items: Set<ItemFromUpdate>, signature: String) =
            Mono.zip(
                    Mono.defer {
                        val realSignature = if (items.isEmpty()) "" else signature
                        podcastRepository.updateSignature(podcast.id, realSignature).then(1.toMono())
                    },
                    items.toFlux()
                            .parallel()
                            .runOn(Schedulers.parallel())
                            .flatMap { podcastRepository.findCover(podcast.id).zipWith(it.toMono()) }
                            .map { (podcastCover, item) -> item.toCreation(podcast.id, podcastCover.toCreation()) }
                            .flatMap { itemRepository.create(it) }
                            .flatMap { item -> fileService.downloadItemCover(item).then(item.toMono()) }
                            .sequential()
                            .collectList()
                            .delayUntil { if (it.isNotEmpty()) podcastRepository.updateLastUpdate(podcast.id) else Mono.empty<Void>() }
            )
}


private fun ItemFromUpdate.toCreation(podcastId: UUID, coverCreation: CoverForCreation) = ItemForCreation(
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
        cover = cover?.toCreation() ?: coverCreation
)

private fun CoverFromUpdate.toCreation() = CoverForCreation(width, height, url)
private fun CoverForPodcast.toCreation() = CoverForCreation(width, height, url)

fun ImageService.fetchCoverUpdateInformationOrOption(url: URI?): Mono<Optional<CoverFromUpdate>> {
    return Mono.justOrEmpty(url)
            .flatMap { fetchCoverInformation(url!!) }
            .map { CoverFromUpdate(it.width, it.height, it.url) }
            .map { Optional.of<CoverFromUpdate>(it) }
            .switchIfEmpty { Optional.empty<CoverFromUpdate>().toMono() }
}
