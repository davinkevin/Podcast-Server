package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.io.path.*

abstract class AbstractDownloader(
    private val downloadRepository: DownloadRepository,
    private val template: MessagingTemplate,
    private val clock: Clock,
    private val file: FileStorageService,
) : Runnable, Downloader {

    private val log = LoggerFactory.getLogger(AbstractDownloader::class.java)

    override lateinit var downloadingInformation: DownloadingInformation
    internal lateinit var itemDownloadManager: ItemDownloadManager
    var target: Path = Files.createTempFile("default-init-file", ".ext")

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        this.downloadingInformation = information
        this.itemDownloadManager = itemDownloadManager
        return this
    }

    override fun run() {
        log.info("Starting download of ${downloadingInformation.item.url}")
        startDownload()
    }

    override fun startDownload() {
        downloadingInformation = downloadingInformation.status(Status.STARTED)
        saveStateOfItem(downloadingInformation.item)
        broadcast(downloadingInformation.item)
        try { download() }
        catch (e: Exception) {
            log.error("Error during download", e)
            this.failDownload()
        }
    }

    override fun stopDownload() {
        downloadingInformation = downloadingInformation.status(Status.STOPPED)
        saveStateOfItem(downloadingInformation.item)
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)

        Files.deleteIfExists(target)

        broadcast(downloadingInformation.item)
    }

    override fun failDownload() {
        downloadingInformation = downloadingInformation
            .status(Status.FAILED)
            .addATry()
        saveStateOfItem(downloadingInformation.item)
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)

        Files.deleteIfExists(target)

        broadcast(downloadingInformation.item)
    }

    override fun finishDownload() {
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)
        Thread.ofVirtual().start(::finishDownloadSync)
    }

    private fun finishDownloadSync() = runCatching {
        file.upload(downloadingInformation.item.podcast.title, target)

        val (mimeType, size) = file.metadata(downloadingInformation.item.podcast.title, target)
            ?: return@runCatching

        downloadRepository.finishDownload(
            id = downloadingInformation.item.id,
            length = size,
            mimeType = mimeType,
            fileName = target.fileName,
            downloadDate = OffsetDateTime.now(clock)
        )
    }
        .onSuccess {
            downloadingInformation = downloadingInformation.status(Status.FINISH)
            broadcast(downloadingInformation.item)
            log.info("End of download for ${downloadingInformation.item.url}")
        }
        .onFailure {
            log.error("Error during download of ${downloadingInformation.item.url}", it)
            failDownload()
        }


    internal fun computeTargetFile(info: DownloadingInformation): Path {
        return computeDestinationFile(info)
            .also { log.debug("Creation of file : {}", it.absolutePathString()) }
    }

    private fun computeDestinationFile(info: DownloadingInformation): Path {
        val simplifiedFilename = info.filename.name
            .replace("\n".toRegex(), "")
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            .let(::Path)

        val name = simplifiedFilename.nameWithoutExtension + "-${downloadingInformation.item.id}"
        val extension = simplifiedFilename.extension

        return Files.createTempDirectory(info.item.podcast.title)
            .resolve("$name.$extension")
    }

    internal fun saveStateOfItem(item: DownloadingItem) {
        Thread.ofVirtual().start { downloadRepository.updateDownloadItem(item) }
    }

    internal fun broadcast(item: DownloadingItem) {
        template.sendItem(item)
    }
}
