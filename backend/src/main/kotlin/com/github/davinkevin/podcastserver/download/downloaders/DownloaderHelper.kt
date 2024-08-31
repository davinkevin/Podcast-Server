package com.github.davinkevin.podcastserver.download.downloaders

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.io.path.*

class DownloaderHelperFactory(
    private val downloadRepository: DownloadRepository,
    private val template: MessagingTemplate,
    private val clock: Clock,
    private val file: FileStorageService,
) {
    fun build(item: DownloadingInformation, manager: ItemDownloadManager) =
        DownloaderHelper(downloadRepository, template, clock, file, manager, item)
}

class DownloaderHelper(
    val downloadRepository: DownloadRepository,
    val template: MessagingTemplate,
    val clock: Clock,
    val file: FileStorageService,
    val manager: ItemDownloadManager,
    var item: DownloadingInformation,
) {

    var target: Path = Files.createTempFile("default-init-file", ".ext")
    private val log = LoggerFactory.getLogger(DownloaderHelper::class.java)

    fun startDownload(download: () -> Unit, failDownload: () -> Unit = this::failDownload) {
        item = item.status(Status.STARTED)
        saveState(item)
        broadcast(item)

        val execution = runCatching { download() }
        if (execution.isFailure) {
            log.error("Error during download", execution.exceptionOrNull())
            failDownload()
        }
    }

    fun stopDownload() {
        item = item.status(Status.STOPPED)
        saveState(item)
        manager.removeACurrentDownload(item.item.id)

        Files.deleteIfExists(target)

        broadcast(item)
    }

    fun failDownload() {
        item = item
            .status(Status.FAILED)
            .addATry()

        saveState(item)

        manager.removeACurrentDownload(item.item.id)

        Files.deleteIfExists(target)

        broadcast(item)
    }

    fun finishDownload() {
        manager.removeACurrentDownload(item.item.id)
        Thread.ofVirtual().start(::finishDownloadSync)
    }

    private fun finishDownloadSync() = runCatching {
        file.upload(UploadRequest.ForItemFromPath(item.item.podcast.title, target))

        val (mimeType, size) = file.metadata(item.item.podcast.title, target)
            ?: return@runCatching

        downloadRepository.finishDownload(
            id = item.item.id,
            length = size,
            mimeType = mimeType,
            fileName = target.fileName,
            downloadDate = OffsetDateTime.now(clock)
        )
    }
        .onSuccess {
            item = item.status(Status.FINISH)
            broadcast(item)
            log.info("End of download for ${item.item.url}")
        }
        .onFailure {
            log.error("Error during download of ${item.item.url}", it)
            failDownload()
        }

    fun computeTargetFile(info: DownloadingInformation): Path {
        val simplifiedFilename = info.filename.name
            .replace("\n".toRegex(), "")
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            .let(::Path)
        val name = simplifiedFilename.nameWithoutExtension + "-${item.item.id}"
        val extension = simplifiedFilename.extension

        return Files.createTempDirectory(info.item.podcast.title)
            .resolve("$name.$extension")
            .also { log.debug("Creation of file : {}", it.absolutePathString()) }
    }
    fun saveState(item: DownloadingInformation) {
        Thread.ofVirtual().start { downloadRepository.updateDownloadItem(item.item) }
    }
    fun broadcast(item: DownloadingInformation) = template.sendItem(item.item)
}