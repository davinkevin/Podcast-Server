package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.manager.downloader.AbstractDownloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Clock
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.math.roundToInt
import kotlin.streams.asSequence

/**
 * Created by kevin on 2019-07-21
 */
class YoutubeDlDownloader(
    downloadRepository: DownloadRepository,
    template: MessagingTemplate,
    clock: Clock,
    file: FileStorageService,
    private val youtubeDl: YoutubeDlService
) : AbstractDownloader(downloadRepository, template, clock, file) {

    private val log = LoggerFactory.getLogger(YoutubeDlDownloader::class.java)

    override fun download(): DownloadingItem {
        val url = downloadingInformation.url.toASCIIString()
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

        Result.run { youtubeDl.download(url, target, callback) }

        finishDownload()

        return downloadingInformation.item
    }

    override fun finishDownload() {
        target = Files.walk(target.parent).asSequence()
                .firstOrNull { it.absolutePathString().startsWith(target.absolutePathString()) }
                ?: throw RuntimeException("No file found after download with youtube-dl...")

        log.debug("File downloaded by youtube-dl is $target")

        super.finishDownload()
    }

    override fun compatibility(downloadingInformation: DownloadingInformation): Int {
        val url = downloadingInformation.urls.first().toASCIIString().lowercase(Locale.getDefault())

        return when {
            downloadingInformation.urls.size > 1 -> Int.MAX_VALUE
            isFromVideoPlatform(url) -> 5
            url.startsWith("http") -> Int.MAX_VALUE - 1
            else -> Int.MAX_VALUE
        }
    }
}
