@file:Suppress("SpringJavaInjectionPointsAutowiringInspection")

package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.manager.selector.ExtractorSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.*
import java.util.function.Predicate.not

@Service
class ItemDownloadManager (
    private val template: MessagingTemplate,
    private val downloadRepository: DownloadRepository,
    podcastServerParameters: PodcastServerParameters,
    private val downloaderSelector: DownloaderSelector,
    private val extractorSelector: ExtractorSelector,
    @param:Qualifier("DownloadExecutor") private val downloadExecutor: ThreadPoolTaskExecutor
) {

    private val log = LoggerFactory.getLogger(ItemDownloadManager::class.java)!!

    val queues = QueueProxy(
        repository = downloadRepository,
        parameters = podcastServerParameters
    )

    val queue: Flux<DownloadingItem>
        get() = queues.waitingQueue.toFlux()

    val downloading: Flux<DownloadingItem>
        get() = queues.downloadingQueue.values
            .map { it.downloadingInformation.item }
            .toFlux()

    /* GETTER & SETTER */
    val limitParallelDownload: Int
        get() = downloadExecutor.corePoolSize

    fun setLimitParallelDownload(limitParallelDownload: Int): Mono<Void> {
        downloadExecutor.corePoolSize = limitParallelDownload
        return manageDownload()
    }

    private fun manageDownload(): Mono<Void> = Mono.defer {
        queues.findAllToDownload(limitParallelDownload)
            .doOnNext(::launchDownloadFor)
            .doOnTerminate(::convertAndSendWaitingQueue)
            .then()
    }

    fun launchDownload(): Mono<Void> {
        return queues.initDownload()
            .then(manageDownload())
    }

    fun stopAllDownload() = queues.allCurrentDownloader().forEach { it.stopDownload() }

    fun addItemToQueue(id: UUID): Mono<Void> {
        return downloadRepository.findDownloadingItemById(id)
            .flatMap(queues::addItemToQueue)
            .then(manageDownload())
    }

    fun removeItemFromQueue(id: UUID, stopItem: Boolean) {
        queues
            .removeItemFromQueue(id, stopItem)
            .subscribeAndTerminateWith(::convertAndSendWaitingQueue)
    }

    fun removeACurrentDownload(id: UUID) {
        queues.removeACurrentDownload(id)
            .then(manageDownload())
            .subscribe()
    }

    private fun launchDownloadFor(item: DownloadingItem) {
        val dlItem = Result.runCatching { extractorSelector.of(item.url).extract(item) }
            .getOrElse {
            log.error("Error during extraction of {}", item.url, it)
            queues.removeItemFromQueue(item.id)
                .then(manageDownload())
                .subscribe()

            return
        }

        val downloader = downloaderSelector.of(dlItem)
            .apply { with(dlItem, this@ItemDownloadManager) }

        queues.addToDownloadingQueue(item, downloader)
            .doOnTerminate { downloadExecutor.execute(downloader) }
            .subscribe()
    }

    fun removeItemFromQueueAndDownload(id: UUID): Mono<Void> {
        val removeFromDownloading = isInDownloadingQueueById(id)
            .filter { it == true }
            .flatMap { queues.findDownloaderOfItem(id) }
            .doOnNext { it.stopDownload() }

        val removeFromWaiting = isInWaitingQueueById(id)
            .filter { it == true }
            .flatMap { queues.removeItemFromQueue(id) }

        return Mono.zip(removeFromDownloading, removeFromWaiting)
            .then()
            .doOnTerminate(::convertAndSendWaitingQueue)
    }

    private fun convertAndSendWaitingQueue(): Sinks.EmitResult {
        return template.sendWaitingQueue(queues.waitingQueue.toList())
    }

    fun isInDownloadingQueueById(id: UUID) = queues.isInDownloadingQueueById(id)
    fun isInWaitingQueueById(id: UUID) = queues.isInWaitingQueueById(id)

    fun moveItemInQueue(itemId: UUID, position: Int) {
        queues.moveItemInQueue(itemId, position)
            .subscribeAndTerminateWith(::convertAndSendWaitingQueue)
    }
}

private fun <T> ArrayDeque<T>.enqueue(item: T): ArrayDeque<T> {
    val joined = this.toMutableList()
    joined.add(item)
    return ArrayDeque(joined)
}
private fun ArrayDeque<DownloadingItem>.delete(id: UUID): ArrayDeque<DownloadingItem> {
    val joined = this.toMutableList().filter { it.id != id }
    return ArrayDeque(joined)
}

class QueueProxy(
    var waitingQueue: ArrayDeque<DownloadingItem> = ArrayDeque<DownloadingItem>(),
    var downloadingQueue: Map<DownloadingItem, Downloader> = mapOf(),
    private val repository: DownloadRepository,
    private val parameters: PodcastServerParameters
) {

    private val log = LoggerFactory.getLogger(QueueProxy::class.java)

    fun initDownload(): Mono<Void> {
        return repository.findAllToDownload(parameters.limitDownloadDate().toOffsetDateTime(), parameters.numberOfTry)
            .filter { it !in waitingQueue }
            .doOnNext {
                log.info("add item to waiting queue")
                waitingQueue = waitingQueue.enqueue(it)
            }
            .then()
    }

    fun allCurrentDownloader() = downloadingQueue.values

    fun findDownloaderOfItem(id: UUID): Mono<Downloader> {
        return downloadingQueue
            .filterKeys { it.id == id }
            .values
            .firstOrNull()
            .toMono()
    }

    fun addItemToQueue(item: DownloadingItem): Mono<Void> {
        if (item in waitingQueue || downloadingQueue.containsKey(item)) {
            return Mono.empty()
        }

        waitingQueue = waitingQueue.enqueue(item)
        return Mono.empty()
    }

    fun removeItemFromQueue(id: UUID): Mono<Void> {
        waitingQueue = waitingQueue.delete(id)
        return Mono.empty()
    }

    fun removeItemFromQueue(id: UUID, hasToBeStopped: Boolean): Mono<Void> {
        val stopItem = if (hasToBeStopped) repository.stopItem(id).then() else Mono.empty()

        return Mono.zip(removeItemFromQueue(id), stopItem)
            .then()
    }

    fun removeACurrentDownload(id: UUID): Mono<Void> {
        downloadingQueue = downloadingQueue
            .filter { (downloadingItem) -> downloadingItem.id != id }

        return Mono.empty()
    }

    fun addToDownloadingQueue(item: DownloadingItem, downloader: Downloader): Mono<Void> {
        return removeItemFromQueue(item.id)
            .then(Mono.defer {
                downloadingQueue = downloadingQueue.filterKeys { it != item } + Pair(item, downloader)
                Mono.empty()
            })
    }

    fun isInDownloadingQueueById(id: UUID) = Mono.just(downloadingQueue.keys.asSequence().map { it.id }.any { it == id })
    fun isInWaitingQueueById(id: UUID): Mono<Boolean> = Mono.just(waitingQueue.asSequence().map { it.id }.any { it == id })

    fun moveItemInQueue(itemId: UUID, position: Int): Mono<Void> {
        val itemToMove = waitingQueue
            .firstOrNull { it.id == itemId }
            ?: error("Moving element in waiting list not authorized : Element wasn't in the list")

        waitingQueue = waitingQueue
            .filter { it.id != itemId }
            .toMutableList()
            .apply { add(position, itemToMove) }
            .let { ArrayDeque(it) }

        return Mono.empty()
    }

    fun findAllToDownload(limit: Int): Flux<DownloadingItem> = Flux.defer {
        val remainingDownloadingSlots = limit - downloadingQueue.size
        waitingQueue
            .toFlux()
            .filter(not(::isStartedOrFinished))
            .take(remainingDownloadingSlots.toLong())
    }
}

private fun <T> Mono<T>.subscribeAndTerminateWith(complete: Runnable) {
    this.subscribe(null, null, complete)
}

private fun isStartedOrFinished(currentItem: DownloadingItem) =
    Status.STARTED == currentItem.status || Status.FINISH == currentItem.status
