@file:Suppress("SpringJavaInjectionPointsAutowiringInspection")

package com.github.davinkevin.podcastserver.manager

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.manager.selector.ExtractorSelector
import com.github.davinkevin.podcastserver.service.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.locks.ReentrantLock

@Service
class ItemDownloadManager (
        private val template: MessagingTemplate,
        private val downloadRepository: DownloadRepository,
        private val podcastServerParameters: PodcastServerParameters,
        private val downloaderSelector: DownloaderSelector,
        private val extractorSelector: ExtractorSelector,
        @param:Qualifier("DownloadExecutor") private val downloadExecutor: ThreadPoolTaskExecutor
) {

    private val log = LoggerFactory.getLogger(ItemDownloadManager::class.java)!!
    private val mainLock = ReentrantLock()

    var waitingQueue = ArrayDeque<DownloadingItem>()
    var downloadingQueue = mapOf<DownloadingItem, Downloader>()

    val queue: Flux<DownloadingItem>
        get() = waitingQueue.toFlux()

    val downloading: Flux<DownloadingItem>
        get() = this.downloadingQueue.values
                .map { it.downloadingInformation.item }
                .toFlux()

    /* GETTER & SETTER */
    val limitParallelDownload: Int
        get() = downloadExecutor.corePoolSize

    val downloadingItems: Set<DownloadingItem>
        get() = this.downloadingQueue.keys

    init {
        Podcast.rootFolder = podcastServerParameters.rootfolder
    }

    fun setLimitParallelDownload(limitParallelDownload: Int) {
        downloadExecutor.corePoolSize = limitParallelDownload
        manageDownload()
    }

    private fun manageDownload() {
        val manageDownloadLock = this.mainLock

        manageDownloadLock.lock()
        try {
            while (this.downloadingQueue.size < downloadExecutor.corePoolSize && !waitingQueue.isEmpty()) {

                val (currentItem, newQueue) = waitingQueue.dequeue()
                waitingQueue = newQueue

                if (!isStartedOrFinished(currentItem)) {
                    launchDownloadFor(currentItem)
                }
            }
        } finally {
            manageDownloadLock.unlock()
        }

        this.convertAndSendWaitingQueue()
    }

    private fun isStartedOrFinished(currentItem: DownloadingItem) =
            Status.STARTED == currentItem.status || Status.FINISH == currentItem.status

    private fun initDownload(): Mono<List<DownloadingItem>> {
        return downloadRepository.findAllToDownload(podcastServerParameters.limitDownloadDate().toOffsetDateTime(), podcastServerParameters.numberOfTry)
                .filter { it !in waitingQueue }
                .collectList()
    }

    fun launchDownload() {
        initDownload().subscribe {
            log.info("add ${it.size} to waiting queue")
            waitingQueue = waitingQueue.enqueueAll(it)
            manageDownload()
        }
    }

    fun stopAllDownload() = this.downloadingQueue.values.forEach { it.stopDownload() }
    fun pauseAllDownload() = this.downloadingQueue.values.forEach { it.pauseDownload() }

    fun restartAllDownload() {
        this.downloadingQueue
                .values
                .toFlux()
                .filter { Status.PAUSED == it.downloadingInformation.item.status }
                .map { it.downloadingInformation.item }
                .subscribe { runAsync { launchDownloadFor(it) } }
    }

    // Change State of id identified download
    fun stopDownload(id: UUID) = findDownloaderOfItem(id)?.stopDownload()
    fun pauseDownload(id: UUID) = findDownloaderOfItem(id)?.pauseDownload()

    private fun findDownloaderOfItem(id: UUID): Downloader? {
        return downloadingQueue
                .filterKeys { it.id == id }
                .values
                .firstOrNull()
    }

    private fun restartDownload(id: UUID) {
        val downloader = findDownloaderOfItem(id) ?: error("downloader not found for item with id $id")
        downloadRepository.findDownloadingItemById(downloader.downloadingInformation.item.id)
                .subscribe { launchDownloadFor(it) }
    }

    fun toggleDownload(id: UUID) {
        val downloader = findDownloaderOfItem(id) ?: error("downloader not found for item with id $id")
        val item = downloader.downloadingInformation.item

        when {
            Status.PAUSED == item.status -> restartDownload(id).also { log.debug("restart du download") }
            Status.STARTED == item.status -> pauseDownload(id).also { log.debug("pause du download") }
        }
    }

    fun addItemToQueue(id: UUID) {
        downloadRepository.findDownloadingItemById(id).subscribe { addItemToQueue(it) }
    }

    internal fun addItemToQueue(item: DownloadingItem) {
        if (item in waitingQueue || isInDownloadingQueue(item)) {
            return
        }

        waitingQueue = waitingQueue.enqueue(item)

        manageDownload()
    }

    fun removeItemFromQueue(id: UUID, stopItem: Boolean) {
        removeItemFromQueue(id)

        if (stopItem) {
            downloadRepository
                    .stopItem(id)
                    .subscribe()
        }

        convertAndSendWaitingQueue()
    }

    private fun removeItemFromQueue(id: UUID) {
        waitingQueue = waitingQueue.delete(id)
    }

    fun removeACurrentDownload(id: UUID) {
        val pairs = this.downloadingQueue.map { it.toPair() }.filter { it.first.id != id }.toTypedArray()
        this.downloadingQueue = mapOf(*pairs)

        manageDownload()
    }

    private fun launchDownloadFor(item: DownloadingItem) {
        when {
            isInDownloadingQueue(item) -> downloadingQueue[item]
                    ?.restartDownload()
                    .also { log.debug("Restart Item : " + item.title) }
            else -> launchWithNewWorkerFrom(item)
        }
    }

    private fun launchWithNewWorkerFrom(item: DownloadingItem) {

        val dlItem = try { extractorSelector.of(item.url).extract(item) }
                catch (e: Exception) {
                    log.error("Error during extraction of {}", item.url, e)
                    manageDownload()
                    return
                }

        val downloader = downloaderSelector.of(dlItem)
                .apply { with(dlItem, this@ItemDownloadManager) }

        this.downloadingQueue = downloadingQueue.filterKeys { it != item } + Pair(item, downloader)
        downloadExecutor.execute(downloader)
    }

    fun removeItemFromQueueAndDownload(id: UUID) {
        when {
            isInDownloadingQueueById(id) -> stopDownload(id)
            isInWaitingQueueById(id) -> removeItemFromQueue(id)
        }
        this.convertAndSendWaitingQueue()
    }

    private fun convertAndSendWaitingQueue() = template.sendWaitingQueue(waitingQueue.toList())

    fun isInDownloadingQueue(item: DownloadingItem) = this.downloadingQueue.containsKey(item)
    fun isInDownloadingQueueById(id: UUID) = this.downloadingQueue.keys.asSequence().map { it.id }.any { it == id }
    fun isInWaitingQueueById(id: UUID) = waitingQueue.asSequence().map { it.id }.any { it == id }

    fun moveItemInQueue(itemId: UUID, position: Int) {
        val itemToMove = waitingQueue
                .firstOrNull { it.id == itemId }
                ?: error("Moving element in waiting list not authorized : Element wasn't in the list")

        waitingQueue = waitingQueue
                .filter { it.id != itemId }
                .toMutableList()
                .apply { add(position, itemToMove) }
                .let { ArrayDeque(it) }

        convertAndSendWaitingQueue()
    }
}

private fun <T> ArrayDeque<T>.dequeue(): Pair<T, ArrayDeque<T>> {
    val i = this.first
    val newQueue = ArrayDeque(this.filter { it !== i })
    return Pair(i, newQueue)
}

private fun <T> ArrayDeque<T>.enqueueAll(e: Collection<T>): ArrayDeque<T> {
    val joined = this.toMutableList()
    joined.addAll(e)
    return ArrayDeque(joined)
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
