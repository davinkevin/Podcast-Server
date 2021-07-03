package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.manager.downloader.AbstractDownloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.sapher.youtubedl.DownloadProgressCallback
import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.commons.io.FilenameUtils.removeExtension
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Clock
import java.util.*
import kotlin.math.roundToInt
import kotlin.streams.asSequence

/**
 * Created by kevin on 2019-07-21
 */
class YoutubeDlDownloader(
        downloadRepository: DownloadRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        clock: Clock,
        private val youtubeDl: YoutubeDlService
) : AbstractDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock) {

    private val log = LoggerFactory.getLogger(YoutubeDlDownloader::class.java)

    override fun download(): DownloadingItem {
        log.info("Starting download of ${downloadingInformation.item.url}")

        val url = downloadingInformation.url()
        downloadingInformation = downloadingInformation.fileName(youtubeDl.extractName(url))

        target = computeTargetFile(downloadingInformation)

        val callback = DownloadProgressCallback { p, _ ->
            val progression = p.roundToInt()
            val broadcast = downloadingInformation.item.progression < progression
            if (broadcast) {
                downloadingInformation = downloadingInformation.progression(progression)
                broadcast(downloadingInformation.item)
            }
        }

        Result.runCatching { youtubeDl.download(url, target!!, callback) }.getOrThrow()

        finishDownload()

        return downloadingInformation.item
    }

    override fun finishDownload() {
        val t = target!!

        val savedPath = Files.walk(t.parent).asSequence()
                .firstOrNull { it.toAbsolutePath().toString().startsWith(t.toAbsolutePath().toString()) }
                ?: throw RuntimeException("No file found after download with youtube-dl...")

        log.debug("File downloaded by youtube-dl is $savedPath")

        if(savedPath != t) {
            val realExtension = getExtension(savedPath.toString())
            val fileNameWithoutAnyExtension = removeExtension(removeExtension(t.fileName.toString()));

            target = t.resolveSibling("$fileNameWithoutAnyExtension.$realExtension$temporaryExtension")
            Result.runCatching { Files.move(savedPath, target!!) }
        }

        super.finishDownload()
    }

    override fun compatibility(downloadingInformation: DownloadingInformation): Int {
        val url = downloadingInformation.urls.first().lowercase(Locale.getDefault())

        return when {
            downloadingInformation.urls.size > 1 -> Int.MAX_VALUE
            isFromVideoPlatform(url) -> 5
            url.startsWith("http") -> Int.MAX_VALUE - 1
            else -> Int.MAX_VALUE
        }
    }
}
