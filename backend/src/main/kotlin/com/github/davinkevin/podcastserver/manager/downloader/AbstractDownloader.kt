package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission.*
import java.time.Clock
import java.time.OffsetDateTime
import kotlin.io.path.absolutePathString

abstract class AbstractDownloader(
    private val downloadRepository: DownloadRepository,
    private val podcastServerParameters: PodcastServerParameters,
    private val template: MessagingTemplate,
    private val mimeTypeService: MimeTypeService,
    private val clock: Clock
) : Runnable, Downloader {

    private val log = LoggerFactory.getLogger(AbstractDownloader::class.java)

    override lateinit var downloadingInformation: DownloadingInformation
    internal lateinit var itemDownloadManager: ItemDownloadManager
    internal lateinit var target: Path

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager) {
        this.downloadingInformation = information
        this.itemDownloadManager = itemDownloadManager
    }

    override fun run() {
        log.debug("Run")
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

        if (this::target.isInitialized) Files.deleteIfExists(target)

        broadcast(downloadingInformation.item)
    }

    override fun failDownload() {
        downloadingInformation = downloadingInformation
            .status(Status.FAILED)
            .addATry()
        saveStateOfItem(downloadingInformation.item)
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)

        if (this::target.isInitialized) Files.deleteIfExists(target)

        broadcast(downloadingInformation.item)
    }

    override fun finishDownload() {
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)

        try {
            val parentLocation = podcastServerParameters.rootfolder
                .resolve(downloadingInformation.item.podcast.title)
                .also(Files::createDirectories)

            val newLocation = parentLocation.resolve(target.fileName)
                .also(Files::deleteIfExists)

            Files.move(target, newLocation)

            target = newLocation ?: error("Error when returning target without extension")
        } catch (e:Exception) {
            failDownload()
            throw RuntimeException("Error during move of file", e)
        }

        try {
            log.debug("Modification of read/write access")
            Files.setPosixFilePermissions(target, setOf(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ))
        } catch (e: Exception) {
            log.warn("Modification of read/write access not made")
        }

        downloadRepository.finishDownload(
            id = downloadingInformation.item.id,
            length = Files.size(target),
            mimeType = mimeTypeService.probeContentType(target),
            fileName = FilenameUtils.getName(target.fileName.toString()),
            downloadDate = OffsetDateTime.now(clock)
        )
            .subscribe()

        downloadingInformation = downloadingInformation.status(Status.FINISH)
        broadcast(downloadingInformation.item)
    }

    internal fun computeTargetFile(info: DownloadingInformation): Path {
        return computeDestinationFile(info)
            .also { log.debug("Creation of file : {}", it.absolutePathString()) }
    }

    private fun computeDestinationFile(info: DownloadingInformation): Path {
        val simplifiedFilename = info.filename
            .replace("\n".toRegex(), "")
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_")

        val name = FilenameUtils.getBaseName(simplifiedFilename) + "-${downloadingInformation.item.id}"
        val extension = FilenameUtils.getExtension(simplifiedFilename)

        return Files.createTempDirectory(info.item.podcast.title)
            .resolve("$name.$extension")
    }

    internal fun saveStateOfItem(item: DownloadingItem) {
        downloadRepository.updateDownloadItem(item).subscribe()
    }

    internal fun broadcast(item: DownloadingItem) {
        template.sendItem(item)
    }
}
