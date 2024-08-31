package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.math.roundToInt
import kotlin.streams.asSequence

class YoutubeDlDownloader(
    private val state: DownloaderHelper,
    private val youtubeDL: YoutubeDlService,
): Downloader {

    private val log = LoggerFactory.getLogger(YoutubeDlDownloader::class.java)

    //* To be removed when Downloader won't be anymore a DownloaderFactory *//
    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader =
        throw IllegalAccessException()

    override fun compatibility(downloadingInformation: DownloadingInformation): Int = throw IllegalAccessException()

    override val downloadingInformation: DownloadingInformation
        get() = state.info
    //* end of section to remove *//

    override fun download(): DownloadingItem {
        val url = state.info.url.toASCIIString()
        state.info = state.info.fileName(youtubeDL.extractName(url))

        state.target = state.computeTargetFile(state.info)

        val callback = DownloadProgressCallback { p, _ ->
            val progression = p.roundToInt()
            val broadcast = state.info.item.progression < progression
            if (broadcast) {
                state.info = state.info.progression(progression)
                state.broadcast(state.info)
            }
        }

        youtubeDL.download(url, state.target, callback)

        finishDownload()

        return state.info.item
    }

    override fun finishDownload() {
        state.target = Files.walk(state.target.parent).asSequence()
                .firstOrNull { it.absolutePathString().startsWith(state.target.absolutePathString()) }
                ?: throw RuntimeException("No file found after download with youtube-dl...")

        log.debug("File downloaded by youtube-dl is {}", state.target)

        state.finishDownload()
    }

    override fun startDownload() = state.startDownload(this)
    override fun stopDownload() = state.stopDownload()
    override fun failDownload() = state.failDownload()
}