package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.attribute.PosixFilePermission.*
import java.time.Clock
import java.time.OffsetDateTime

public abstract class AbstractDownloader(
        private val downloadRepository: DownloadRepository,
        private val podcastServerParameters: PodcastServerParameters,
        private val template: MessagingTemplate,
        private val mimeTypeService: MimeTypeService,
        private val clock: Clock
) : Runnable, Downloader {

    private val log = LoggerFactory.getLogger(AbstractDownloader::class.java)

    override lateinit var downloadingInformation: DownloadingInformation
    internal lateinit var itemDownloadManager: ItemDownloadManager

    internal var target: Path? = null

    internal val temporaryExtension: String = podcastServerParameters.downloadExtension
    private val hasTempExtensionMatcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:*$temporaryExtension")

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

    override fun pauseDownload() {
        downloadingInformation = downloadingInformation.status(Status.PAUSED)
        saveStateOfItem(downloadingInformation.item)
        broadcast(downloadingInformation.item)
    }

    override fun stopDownload() {
        downloadingInformation = downloadingInformation.status(Status.STOPPED)
        saveStateOfItem(downloadingInformation.item)
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)

        target?.let { Files.deleteIfExists(it) }

        broadcast(downloadingInformation.item)
    }

    override fun failDownload() {
        downloadingInformation = downloadingInformation
                .status(Status.FAILED)
                .addATry()
        saveStateOfItem(downloadingInformation.item)
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)

        target?.let { Files.deleteIfExists(it) }

        broadcast(downloadingInformation.item)
    }

    override fun finishDownload() {
        itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)
        var t = target

        if (t == null) {
            failDownload()
            return
        }

        try {
            if (hasTempExtensionMatcher.matches(t.fileName)) {
                val targetWithoutExtension = t.resolveSibling(t.fileName.toString().replace(temporaryExtension, ""))

                Files.deleteIfExists(targetWithoutExtension)
                Files.move(t, targetWithoutExtension)

                t = targetWithoutExtension ?: error("Error when returning target without extension")
            }
        } catch (e:Exception) {
            failDownload()
            throw RuntimeException("Error during move of file", e)
        }

        try {
            log.debug("Modification of read/write access")
            Files.setPosixFilePermissions(t, setOf(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ))
        } catch (e: Exception) {
            log.warn("Modification of read/write access not made")
        }

        downloadRepository.finishDownload(
                id = downloadingInformation.item.id,
                length = Files.size(t),
                mimeType = mimeTypeService.probeContentType(t),
                fileName = FilenameUtils.getName(t.fileName.toString()),
                downloadDate = OffsetDateTime.now(clock)
        )
                .subscribe()

        target = t

        downloadingInformation = downloadingInformation.status(Status.FINISH)
        broadcast(downloadingInformation.item)
    }

    internal fun computeTargetFile(info: DownloadingInformation): Path = runCatching {
        val t = target
        if (t != null) return t

        val finalFile = computeDestinationFile(info)
        log.debug("Creation of file : {}", finalFile.toFile().absolutePath)

        if (!Files.exists(finalFile.parent)) {
            Files.createDirectories(finalFile.parent)
        }

        return finalFile.resolveSibling(finalFile.fileName.toString() + temporaryExtension)
    }.getOrElse {
        failDownload()
        throw RuntimeException("Error during creation of target file", it)
    }

    private fun computeDestinationFile(info: DownloadingInformation): Path {
        val simplifiedFilename = info.filename
                .replace("\n".toRegex(), "")
                .replace("[^a-zA-Z0-9.-]".toRegex(), "_")

        val name = FilenameUtils.getBaseName(simplifiedFilename) + "-${downloadingInformation.item.id}"
        val extension = FilenameUtils.getExtension(simplifiedFilename)

        return podcastServerParameters.rootfolder.resolve(info.item.podcast.title).resolve("$name.$extension")
    }

    internal fun saveStateOfItem(item: DownloadingItem) {
        downloadRepository.updateDownloadItem(item).subscribe()
    }

    internal fun broadcast(item: DownloadingItem) {
        template.sendItem(item)
    }
}
