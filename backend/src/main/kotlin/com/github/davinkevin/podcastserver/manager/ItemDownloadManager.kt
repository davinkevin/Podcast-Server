package com.github.davinkevin.podcastserver.manager

import arrow.core.Failure
import arrow.core.Success
import arrow.core.getOrElse
import arrow.data.mapOf
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.manager.selector.ExtractorSelector
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.collection.HashMap
import io.vavr.collection.Queue
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.entity.Status
import lan.dk.podcastserver.repository.ItemRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.locks.ReentrantLock

@Service
@Transactional
class ItemDownloadManager (
        private val template: SimpMessagingTemplate,
        private val itemRepository: ItemRepository,
        private val podcastServerParameters: PodcastServerParameters,
        private val downloaderSelector: DownloaderSelector,
        private val extractorSelector: ExtractorSelector,
        @param:Qualifier("DownloadExecutor") private val downloadExecutor: ThreadPoolTaskExecutor
) {

    private val log = LoggerFactory.getLogger(ItemDownloadManager::class.java)!!
    private val mainLock = ReentrantLock()

    var _waitingQueue = ArrayDeque<Item>()
    var _downloadingQueue = mapOf<Item, Downloader>()

    val waitingQueue: Queue<Item>
        get() = Queue.ofAll(_waitingQueue)

    val downloadingQueue: io.vavr.collection.Map<Item, Downloader>
        get() = HashMap.ofAll(_downloadingQueue)

    /* GETTER & SETTER */
    val limitParallelDownload: Int
        get() = downloadExecutor.corePoolSize

    val numberOfCurrentDownload: Int
        get() = _downloadingQueue.size

    val itemsInDownloadingQueue: io.vavr.collection.Set<Item>
        get() = _downloadingQueue.keys.toVΛVΓ()

    init {
        Item.rootFolder = podcastServerParameters.rootfolder
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
            while (_downloadingQueue.size < downloadExecutor.corePoolSize && !_waitingQueue.isEmpty()) {

                val (currentItem, newQueue) = _waitingQueue.dequeue()
                _waitingQueue = newQueue

                if (!isStartedOrFinished(currentItem)) {
                    getDownloaderByTypeAndRun(currentItem)
                }
            }
        } finally {
            manageDownloadLock.unlock()
        }

        this.convertAndSendWaitingQueue()
    }

    private fun isStartedOrFinished(currentItem: Item) =
            Status.STARTED == currentItem.status || Status.FINISH == currentItem.status

    private fun initDownload() {
        val newItemsToEnqueue = itemRepository.findAllToDownload(podcastServerParameters.limitDownloadDate(), podcastServerParameters.numberOfTry)
                .filter { it !in _waitingQueue }
                .toJavaSet()

        _waitingQueue = _waitingQueue.enqueueAll(newItemsToEnqueue)
    }

    fun launchDownload() {
        this.initDownload()
        this.manageDownload()
    }

    fun stopAllDownload() = _downloadingQueue.values.forEach { it.stopDownload() }
    fun pauseAllDownload() = _downloadingQueue.values.forEach { it.pauseDownload() }

    fun restartAllDownload() {
        _downloadingQueue
                .values
                .filter { Status.PAUSED == it.item.status }
                .forEach { d -> runAsync { getDownloaderByTypeAndRun(d.item) } }
    }

    // Change State of id identified download
    fun stopDownload(id: UUID) = getDownloaderOfItemWithId(id).toList().forEach { it.stopDownload() }
    fun pauseDownload(id: UUID) = getDownloaderOfItemWithId(id).toList().forEach { it.pauseDownload() }

    private fun getDownloaderOfItemWithId(id: UUID) =
            _downloadingQueue
                    .entries
                    .firstOption { it.key.id == id }
                    .map { it.value }

    fun restartDownload(id: UUID) {
        getDownloaderOfItemWithId(id)
                .map { it.item }
                .toList()
                .forEach { getDownloaderByTypeAndRun(it) }
    }

    fun toggleDownload(id: UUID) {
        val item = getDownloaderOfItemWithId(id)
                .map { it.item }
                .getOrElse { throw RuntimeException("Item not with $id not found in download list") }

        when {
            Status.PAUSED == item.status -> restartDownload(id).also { log.debug("restart du download") }
            Status.STARTED == item.status -> pauseDownload(id).also { log.debug("pause du download") }
        }
    }

    fun addItemToQueue(id: UUID) =
            addItemToQueue(itemRepository.findById(id).orElseThrow { RuntimeException("Item with ID $id not found") })

    fun addItemToQueue(item: Item) {
        if (item in _waitingQueue || isInDownloadingQueue(item)) {
            return
        }

        _waitingQueue = _waitingQueue.enqueue(item)

        manageDownload()
    }

    @Transactional
    fun removeItemFromQueue(id: UUID, stopItem: Boolean) {
        val item = itemRepository.findById(id).orElseThrow { RuntimeException("Item with ID $id not found") }
        removeItemFromQueue(item)

        if (stopItem) {
            itemRepository.save(item.apply { status = Status.STOPPED })
        }

        convertAndSendWaitingQueue()
    }

    private fun removeItemFromQueue(item: Item) {
        _waitingQueue = _waitingQueue.delete(item)
    }

    fun removeACurrentDownload(item: Item) {
        val pairs = _downloadingQueue.map { it.toPair() }.filter { it.first != item }.toTypedArray()
        _downloadingQueue = mapOf(*pairs)

        manageDownload()
    }

    fun getItemInDownloadingQueue(id: UUID): Item {
        return _downloadingQueue
                .keys
                .firstOption { it.id == id}
                .getOrElse { Item.DEFAULT_ITEM }
    }

    private fun getDownloaderByTypeAndRun(item: Item) {
        when {
            isInDownloadingQueue(item) -> (_downloadingQueue[item] ?: DownloaderSelector.NO_OP_DOWNLOADER)
                    .restartDownload()
                    .also { log.debug("Restart Item : " + item.title) }

            else -> launchWithNewWorkerFrom(item)
        }
    }

    private fun launchWithNewWorkerFrom(item: Item) {
        val downloadingItem = arrow.core.Try { this.extractorSelector.of(item.url).extract(item) }

        val dlItem = when(downloadingItem) {
            is Failure -> {
                log.error("Error during extraction of {}", item.url, downloadingItem.exception)
                manageDownload()
                return
            }
            is Success -> downloadingItem.value
        }

        val downloader = downloaderSelector.of(dlItem)
                .apply { with(dlItem, this@ItemDownloadManager) }

        val pairs = _downloadingQueue.map { it.toPair() }.filter { it.first != item }
                .toMutableList()
                .apply { this.add(Pair(item, downloader)) }
                .toTypedArray()

        _downloadingQueue = mapOf(*pairs)
        downloadExecutor.execute(downloader)
    }

    fun resetDownload(item: Item) {
        if (!isInDownloadingQueue(item) || !canBeReset(item))
            return

        item.addATry()
        launchWithNewWorkerFrom(item)
    }

    fun removeItemFromQueueAndDownload(itemToRemove: Item) {
        when {
            isInDownloadingQueue(itemToRemove) -> stopDownload(itemToRemove.id)
            itemToRemove in _waitingQueue -> removeItemFromQueue(itemToRemove)
        }
        this.convertAndSendWaitingQueue()
    }

    private fun convertAndSendWaitingQueue() = template.convertAndSend(WS_TOPIC_WAITING_LIST, waitingQueue)

    fun canBeReset(item: Item) = item.numberOfFail + 1 <= podcastServerParameters.numberOfTry

    fun isInDownloadingQueue(item: Item) = _downloadingQueue.containsKey(item)

    fun moveItemInQueue(itemId: UUID, position: Int) {
        val copyWL = _waitingQueue.toMutableList()

        val itemToMove = copyWL
                .firstOption { it.id == itemId }
                .getOrElse { throw RuntimeException("Moving element in waiting list not authorized : Element wasn't in the list") }

        val reorderList = copyWL.asSequence().filter { it.id != itemId }.toMutableList()
        reorderList.add(position, itemToMove)

        _waitingQueue = ArrayDeque(reorderList)

        convertAndSendWaitingQueue()
    }

    fun clearWaitingQueue() {
        _waitingQueue = ArrayDeque()
    }

    companion object {
        private const val WS_TOPIC_WAITING_LIST = "/topic/waiting"
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
private fun <T> ArrayDeque<T>.delete(item: T): ArrayDeque<T> {
    val joined = this.toMutableList().filter { it == item }
    return ArrayDeque(joined)
}