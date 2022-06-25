@file:Suppress("SpringJavaInjectionPointsAutowiringInspection")

package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

@Service
class ItemDownloadManager (
    private val template: MessagingTemplate,
    private val repository: DownloadRepository,
    private val parameters: PodcastServerParameters,
    private val downloaderSelector: DownloaderSelector,
    private val downloadExecutor: ThreadPoolTaskExecutor
) {

    private var downloaders = ConcurrentHashMap<UUID, Downloader>()

    val queue: Flux<DownloadingItem>
        get() = repository.findAllWaiting()

    val downloading: Flux<DownloadingItem>
        get() = repository.findAllDownloading()

    var limitParallelDownload: Int
        get() = downloadExecutor.corePoolSize
        set(value) {
            downloadExecutor.corePoolSize = value
            manageDownload().subscribe()
        }

    fun launchDownload(): Mono<Void> {
        return repository
            .initQueue(parameters.limitDownloadDate(), parameters.numberOfTry)
            .then(manageDownload())
    }

    private fun manageDownload(): Mono<Void> = Mono.defer {
        repository
            .findAllToDownload(limitParallelDownload)
            .delayUntil(::launchDownloadFor)
            .doOnEach { convertAndSendWaitingQueue() }
            .then()
    }

    private fun launchDownloadFor(item: DownloadingItem): Mono<Void> {
        val dlItem = item.toInformation()

        val downloader = downloaderSelector.of(dlItem)
            .with(dlItem, this@ItemDownloadManager)

        downloaders[item.id] = downloader

        return repository.startItem(item.id)
            .doOnTerminate { downloadExecutor.execute(downloader) }
    }

    fun stopAllDownload() = downloaders.forEach { it.value.stopDownload() }

    fun addItemToQueue(id: UUID): Mono<Void> {
        return repository.addItemToQueue(id)
            .then(manageDownload())
    }

    fun removeItemFromQueue(id: UUID, stopItem: Boolean) {
        repository.remove(id, stopItem)
            .then(manageDownload())
            .subscribe()
    }

    fun removeACurrentDownload(id: UUID) {
        downloaders.remove(id)
        repository.remove(id, false)
            .then(manageDownload())
            .subscribe()
    }

    fun removeItemFromQueueAndDownload(id: UUID): Mono<Void> {
        val downloader = downloaders[id]
        if (downloader != null) {
            downloader.stopDownload()
            return Mono.empty()
        }

        return repository.remove(id, false)
            .then(manageDownload())
    }

    private fun convertAndSendWaitingQueue() {
        repository
            .findAllWaiting()
            .collectList()
            .subscribe { template.sendWaitingQueue(it) }
    }

    fun isInDownloadingQueueById(id: UUID): Mono<Boolean> = downloaders.containsKey(id).toMono()

    fun moveItemInQueue(id: UUID, position: Int): Mono<Void> {
        return repository.moveItemInQueue(id, position)
            .doOnTerminate { convertAndSendWaitingQueue() }
    }
}

private fun DownloadingItem.toInformation(): DownloadingInformation {
    val fileName = Path(url.path).fileName
    return DownloadingInformation(this, listOf(url), fileName, null)
}
