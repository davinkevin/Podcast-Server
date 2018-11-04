package com.github.davinkevin.podcastserver.manager.downloader

import com.github.axet.vget.VGet
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import com.github.axet.wget.info.ex.DownloadIOCodeError
import com.github.axet.wget.info.ex.DownloadMultipartError
import com.github.davinkevin.podcastserver.service.FfmpegService
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.factory.WGetFactory
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import io.vavr.API.Try
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Status
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.lang.String.format
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.ZonedDateTime.now
import java.time.temporal.TemporalAmount
import java.util.Objects.nonNull
import java.util.concurrent.CountDownLatch

/**
 * Created by kevin on 14/12/2013 for Podcast Server
 */

@Component
@Scope(SCOPE_PROTOTYPE)
class YoutubeDownloader(
        itemRepository: ItemRepository,
        podcastRepository: PodcastRepository,
        podcastServerParameters: PodcastServerParameters,
        template: SimpMessagingTemplate,
        mimeTypeService: MimeTypeService,
        val wGetFactory: WGetFactory,
        val ffmpegService: FfmpegService
) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

    private val log = LoggerFactory.getLogger(YoutubeDownloader::class.java)

    lateinit var v: VGet

    private lateinit var watcher: YoutubeWatcher

    override fun download(): Item {
        try {
            watcher = YoutubeWatcher(this)
            val parser = wGetFactory.parser(item.url)

            v = wGetFactory.newVGet(parser.info(URL(item.url)))
            v.extract(parser, stopDownloading, watcher)

            target = getTargetFile(item)

            v
                    .video
                    .info
                    .forEach { vi -> vi.target = generatePartFile(target!!, vi).toFile() }

            v.download(parser, stopDownloading, watcher)

        } catch (e: Exception) {
            if (e is DownloadMultipartError) {
                e.info
                        .parts
                        .stream()
                        .map { it.exception }
                        .filter { it != null }
                        .forEach { it.printStackTrace() }
            }
            throw e
        }

        log.debug("Download ended")
        return item
    }

    private fun generatePartFile(targetFile: Path, vi: VideoFileInfo): Path {
        return targetFile.resolveSibling(targetFile.fileName.toString() + v.getContentExt(vi))
    }

    override fun getFileName(item: Item) =
            v.video?.title?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?:
            throw RuntimeException("Error during creation of filename of " + item.url)

    override fun pauseDownload() {
        item.status = Status.PAUSED
        saveSyncWithPodcast()
        convertAndSaveBroadcast()
    }

    override fun restartDownload() {
        item.status = Status.STARTED
        saveSyncWithPodcast()
        convertAndSaveBroadcast()
        watcher.lock.countDown()
    }

    override fun stopDownload() {
        if (item.status == Status.PAUSED) {
            watcher.lock.countDown()
        }
        super.stopDownload()

        if (nonNull(v) && nonNull(v.video) && nonNull(v.video.info))
            v.video
                    .info
                    .stream()
                    .filter { v -> nonNull(v.targetFile) }
                    .map { v -> v.targetFile.toPath() }
                    .forEach { p -> Try { Files.deleteIfExists(p) } }
    }

    override fun finishDownload() {
        val fileWithExtension = target!!.resolveSibling(toDefinitiveFileName())
        Files.deleteIfExists(target)

        target =
                if (hasOnlyOneStream()) {
                    try { Files.move(v.video.info[0].targetFile.toPath(), fileWithExtension, StandardCopyOption.REPLACE_EXISTING) }
                    catch (e: Exception) { failDownload(); throw RuntimeException("Error during specific move", e) }
                } else {
                    val audioFile = getStream("audio")
                    val video = getStream("video")
                    try { ffmpegService.mergeAudioAndVideo(video, audioFile, fileWithExtension)
                    } catch (e: Exception) { failDownload(); throw RuntimeException("Error during specific move", e)
                    } finally {
                        Files.deleteIfExists(video)
                        Files.deleteIfExists(audioFile)
                    }
                }
        super.finishDownload()
    }

    private fun toDefinitiveFileName(): String {
        val videoExt = v.video.info.asSequence()
                .map { it.contentType }
                .filter { it.contains("video") }
                .map { it.substringAfter("/") }
                .firstOrNull() ?: DEFAULT_EXTENSION_MP4

        return target!!.fileName.toString().replace(temporaryExtension, ".$videoExt")
    }

    private fun hasOnlyOneStream() = v.video.info.size == 1

    private fun getStream(type: String) =
            v.video.info.asSequence()
                    .filter { v -> v.contentType.contains(type) }
                    .map { v -> v.targetFile.toPath() }
                    .firstOrNull() ?: throw RuntimeException(format(ERROR_NO_CONTENT_TYPE, type, item.title, item.url))

    override fun compatibility(downloadingItem: DownloadingItem) =
            if (downloadingItem.urls.size == 1 && "youtube.com" in downloadingItem.urls.first()) 1
            else Integer.MAX_VALUE

    internal class YoutubeWatcher(private val youtubeDownloader: YoutubeDownloader, private val maxWaitingTime: TemporalAmount = Duration.ofMinutes(5)) : Runnable {

        private val log = LoggerFactory.getLogger(YoutubeWatcher::class.java)
        var lock = CountDownLatch(1)

        private val launchDateDownload = now()
        private var globalSize: Long = -1L

        override fun run() {
            val info = youtubeDownloader.v.video
            val downloadInfo = info.info
            val item = youtubeDownloader.item

            when (info.state) {
                VideoInfo.States.EXTRACTING_DONE -> log.debug(FilenameUtils.getName(item.url.toString()) + " " + info.state)
                VideoInfo.States.ERROR -> youtubeDownloader.failDownload()
                VideoInfo.States.DONE -> done(downloadInfo, item)
                VideoInfo.States.RETRYING -> retrying(info)
                VideoInfo.States.DOWNLOADING -> downloading(downloadInfo, item)
                VideoInfo.States.STOP -> log.debug("Pause / Stop download")
                else -> {}
            }

            if (item.status == Status.PAUSED) {
                awaitAndRestart()
            }
        }

        private fun retrying(info: VideoInfo) {
            val exception = info.exception

            log.debug(info.state.toString() + " " + info.delay)
            if (info.delay == 0) {
                log.error(exception.toString())
            }
            if (exception is DownloadIOCodeError) {
                log.debug("Cause  : " + exception.code)
            }

            if (launchDateDownload.isBefore(now().minus(maxWaitingTime))) {
                youtubeDownloader.failDownload()
            }
        }

        private fun downloading(downloadInfo: List<VideoFileInfo>, item: Item) {
            if (globalSize == -1L) {
                globalSize = downloadInfo.filter { it.length != null }.map { it.length }.sum()
            }

            val count = downloadInfo.map { it.count }.sum()
            val currentState = (count * 100 / globalSize.toFloat()).toInt()
            if (item.progression < currentState) {
                item.progression = currentState
                log.debug("{} - {}%", item.title, item.progression)
                youtubeDownloader.convertAndSaveBroadcast()
            }
        }

        private fun done(downloadInfo: List<VideoFileInfo>, item: Item) {
            downloadInfo
                    .stream()
                    .map { it.targetFile }
                    .filter { it != null }
                    .forEach { log.debug("{} - Téléchargement terminé", FilenameUtils.getName(it.absolutePath)) }

            if (item.status == Status.STARTED)
                youtubeDownloader.finishDownload()
        }

        private fun awaitAndRestart() {
            lock.await()
            lock = CountDownLatch(1)
        }
    }

    companion object {
        private const val DEFAULT_EXTENSION_MP4 = "mp4"
        private const val ERROR_NO_CONTENT_TYPE = "Content Type %s not found for video %s at url %s"
    }
}
