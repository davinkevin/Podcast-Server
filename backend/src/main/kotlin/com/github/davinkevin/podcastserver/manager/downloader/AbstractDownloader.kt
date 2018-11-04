package com.github.davinkevin.podcastserver.manager.downloader


import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Status
import lan.dk.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.transaction.annotation.Transactional
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

abstract class AbstractDownloader(
        val itemRepository: ItemRepository,
        val podcastRepository: PodcastRepository,
        val podcastServerParameters: PodcastServerParameters,
        val template: SimpMessagingTemplate,
        val mimeTypeService: MimeTypeService
) : Runnable, Downloader {

    private val log = LoggerFactory.getLogger(AbstractDownloader::class.java)

    override lateinit var item: Item
    internal lateinit var downloadingItem: DownloadingItem
    internal lateinit var itemDownloadManager: ItemDownloadManager

    internal lateinit var temporaryExtension: String
    private lateinit var hasTempExtensionMatcher: PathMatcher
    internal var target: Path? = null
    internal var stopDownloading = AtomicBoolean(false)

    override fun with(item: DownloadingItem, itemDownloadManager: ItemDownloadManager) {
        this.downloadingItem = item
        this.item = downloadingItem.item
        this.itemDownloadManager = itemDownloadManager
    }

    override fun run() {
        log.debug("Run")
        startDownload()
    }

    override fun startDownload() {
        item.status = Status.STARTED
        stopDownloading.set(false)
        saveSyncWithPodcast()
        convertAndSaveBroadcast()
        try { download() }
        catch (e: Exception) {
            log.error("Error during download", e)
            this.failDownload()
        }
    }

    override fun pauseDownload() {
        item.status = Status.PAUSED
        stopDownloading.set(true)
        saveSyncWithPodcast()
        convertAndSaveBroadcast()
    }

    override fun stopDownload() {
        item.status = Status.STOPPED
        stopDownloading.set(true)
        saveSyncWithPodcast()
        itemDownloadManager.removeACurrentDownload(item)

        if (target != null)
            Files.deleteIfExists(target)

        convertAndSaveBroadcast()
    }

    override fun failDownload() {
        item.status = Status.FAILED
        stopDownloading.set(true)
        item.addATry()
        saveSyncWithPodcast()
        itemDownloadManager.removeACurrentDownload(item)
        if (target != null)
            Files.deleteIfExists(target)
        convertAndSaveBroadcast()
    }

    @Transactional
    override fun finishDownload() {
        itemDownloadManager.removeACurrentDownload(item)
        var t = target

        if (t == null) {
            failDownload()
            return
        }

        try {
            if (hasTempExtensionMatcher.matches(t.fileName)) {
                val targetWithoutExtension = t.resolveSibling(t.fileName.toString().replace(temporaryExtension, ""))!!

                Files.deleteIfExists(targetWithoutExtension)
                Files.move(t, targetWithoutExtension)

                t = targetWithoutExtension
            }
        } catch (e:Exception) {
            failDownload()
            throw RuntimeException("Error during move of file", e)
        }

        item.apply {
            status = Status.FINISH
            length = Files.size(t)
            mimeType = mimeTypeService.probeContentType(t)
            fileName = FilenameUtils.getName(t.fileName.toString())
            downloadDate = ZonedDateTime.now()
        }

        target = t
        saveSyncWithPodcast()
        convertAndSaveBroadcast()
    }

    @Transactional
    open fun getTargetFile(item: Item): Path {
        val t = target

        if (t != null) return t

        val finalFile = getDestinationFile(item)
        log.debug("Creation of file : {}", finalFile.toFile().absolutePath)

        try {
            if (Files.notExists(finalFile.parent)) Files.createDirectories(finalFile.parent)

            if (!(Files.exists(finalFile) || Files.exists(finalFile.resolveSibling(finalFile.fileName.toString() + temporaryExtension)))) {
                return finalFile.resolveSibling(finalFile.fileName.toString() + temporaryExtension)
            }

            log.info("Doublon sur le fichier en lien avec {} - {}, {}", item.podcast.title, item.id, item.title)
            return generateTempFileNextTo(finalFile)
        } catch (e: Exception) {
            failDownload()
            throw RuntimeException("Error during creation of target file", e)
        }
    }

    /* Change visibility after kotlin Migration */
    fun generateTempFileNextTo(finalFile: Path): Path {
        val fileName = finalFile.fileName.toString()
        val name = FilenameUtils.getBaseName(fileName)
        val extension = FilenameUtils.getExtension(fileName)
        return Files.createTempFile(finalFile.parent, "$name-", ".$extension$temporaryExtension")
    }

    private fun getDestinationFile(item: Item): Path {
        val fileName = downloadingItem.filename ?: getFileName(item)
        return podcastServerParameters.rootfolder.resolve(item.podcast.title).resolve(fileName)
    }

    @Transactional
    open fun saveSyncWithPodcast() {
        try {
            val podcast = podcastRepository.findById(item.podcast.id).orElseThrow { RuntimeException("Podcast with ID " + item.podcast.id + " not found") }
            item.podcast = podcast
            itemRepository.save(item)
        } catch (e: Exception) {
            log.error("Error during save and Sync of the item {}", item, e)
        }
    }

    @Transactional
    open fun convertAndSaveBroadcast() {
        template.convertAndSend(WS_TOPIC_DOWNLOAD, item)
    }

    override fun getItemUrl(item: Item) = downloadingItem.url().getOrElse { item.url }!!

    @PostConstruct
    fun postConstruct() {
        temporaryExtension = podcastServerParameters.downloadExtension
        hasTempExtensionMatcher = FileSystems.getDefault().getPathMatcher("glob:*$temporaryExtension")
    }

    companion object {
        /* Change visibility after kotlin Migration */  const val WS_TOPIC_DOWNLOAD = "/topic/download"
    }
}
