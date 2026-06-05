package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.slf4j.errorWithDebugStack
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

    private var process: Process? = null

    override val downloadingInformation: DownloadingInformation
        get() = state.info

    override fun download(): DownloadingItem {
        val url = state.info.url.toASCIIString()
        state.info = state.info.fileName(youtubeDL.extractName(url))

        state.target = state.computeTargetFile(state.info)

        val callback = DownloadProgressCallback { p ->
            val progression = p.roundToInt()
            val broadcast = state.info.item.progression < progression
            if (broadcast) {
                state.info = state.info.progression(progression)
                state.broadcast(state.info)
            }
        }

        runCatching { youtubeDL.download(url, state.target, callback) { process = it } }
            .getOrElse {
                // a stop request kills the yt-dlp process, which surfaces here
                // as a failed execution: keep the STOPPED state in that case
                if (state.info.item.status == Status.STOPPED) return state.info.item
                throw it
            }

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

    override fun stopDownload() {
        try {
            state.stopDownload()
            process?.destroy()
        } catch (e: Exception) {
            log.errorWithDebugStack("Error during stop of yt-dlp process :", throwable = e)
            state.failDownload()
        }
    }

    override fun failDownload() = state.failDownload()
}