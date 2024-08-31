@file:Suppress("SpringJavaInjectionPointsAutowiringInspection")

package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

class ItemDownloadManager (
    private val template: MessagingTemplate,
    private val repository: DownloadRepository,
    private val parameters: PodcastServerParameters,
    private val downloaderSelector: DownloaderSelector,
    private val downloadExecutor: ThreadPoolTaskExecutor
) {

    private var downloaders = ConcurrentHashMap<UUID, Downloader>()

    val queue: List<DownloadingItem>
        get() = repository.findAllWaiting()

    val downloading: List<DownloadingItem>
        get() = repository.findAllDownloading()

    var limitParallelDownload: Int
        get() = downloadExecutor.corePoolSize
        set(value) {
            downloadExecutor.corePoolSize = value
            manageInBackground()
        }

    fun launchDownload() {
        repository.initQueue(parameters.limitDownloadDate(), parameters.numberOfTry)
        manageInBackground()
    }

    private fun manageDownload() {
        val allToDownload = repository
            .findAllToDownload(limitParallelDownload)

        allToDownload.forEach(::launchDownloadFor)

        convertAndSendWaitingQueueInBackground()
    }

    private fun launchDownloadFor(item: DownloadingItem) {
        val dlItem = item.toInformation()

        val downloader = downloaderSelector.of(dlItem)
            .with(dlItem, this@ItemDownloadManager)

        downloaders[item.id] = downloader

        repository.startItem(item.id)
        downloadExecutor.execute(downloader)
    }

    fun stopAllDownload() {
        downloaders.forEach { it.value.stopDownload() }
    }

    fun addItemToQueue(id: UUID) {
        repository.addItemToQueue(id)
        manageInBackground()
    }

    fun removeItemFromQueue(id: UUID, stopItem: Boolean) {
        repository.remove(id, stopItem)
        manageInBackground()
    }

    fun removeACurrentDownload(id: UUID) {
        downloaders.remove(id)
        repository.remove(id, false)
        manageInBackground()
    }

    fun removeItemFromQueueAndDownload(id: UUID) {
        val downloader = downloaders[id]
        if (downloader != null) {
            downloader.stopDownload()
            return
        }

        repository.remove(id, false)
        manageInBackground()
    }

    private fun manageInBackground() {
        Thread.ofVirtual().start(::manageDownload)
    }

    private fun convertAndSendWaitingQueueInBackground() {
        Thread.ofVirtual().start(::convertAndSendWaitingQueue)
    }

    private fun convertAndSendWaitingQueue() {
        val list = repository.findAllWaiting()

        template.sendWaitingQueue(list)
    }

    fun isInDownloadingQueueById(id: UUID): Boolean {
        return downloaders.containsKey(id)
    }

    fun moveItemInQueue(id: UUID, position: Int) {
        repository.moveItemInQueue(id, position)

        convertAndSendWaitingQueueInBackground()
    }
}

private fun DownloadingItem.toInformation(): DownloadingInformation {
    val fileName = Path(url.path).fileName
    return DownloadingInformation(this, listOf(url), fileName, null)
}
