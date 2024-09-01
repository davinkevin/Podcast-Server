package com.github.davinkevin.podcastserver.download.downloaders.rtmp

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class RTMPDownloader(
    private val state: DownloaderHelper,
    private val processService: ProcessService,
    private val externalTools: ExternalTools,
) : Downloader {

    private val log = LoggerFactory.getLogger(RTMPDownloader::class.java)

    //* To be removed when Downloader won't be anymore a DownloaderFactory *//
    override val downloadingInformation: DownloadingInformation
        get() = state.info
    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader =
        throw IllegalAccessException()
    override fun compatibility(downloadingInformation: DownloadingInformation) =
        if (downloadingInformation.urls.size == 1 && downloadingInformation.urls.first().toASCIIString().startsWith("rtmp://")) 1
        else Integer.MAX_VALUE
    //* end of section to remove *//

    private var pid = 0L
    private var p: Process? = null
    private val stopDownloading = AtomicBoolean(false)

    override fun download(): DownloadingItem {
        log.debug("Download")

        try {
            state.target = state.computeTargetFile(downloadingInformation)
            log.debug("out file : {}", state.target.toAbsolutePath().toString())

            val processToExecute = processService
                .newProcessBuilder(
                    externalTools.rtmpdump,
                    "-r",
                    downloadingInformation.url.toASCIIString(),
                    "-o",
                    state.target.toAbsolutePath().toString()
                )
                .directory(File("/tmp"))
                .redirectErrorStream(true)

            p = processService.start(processToExecute)
            pid = processService.pidOf(p!!)

            listenLogs(p!!)

            p!!.waitFor()
            pid = 0
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        return downloadingInformation.item
    }

    fun listenLogs(process: Process) {
        log.debug("Reading output of RTMPDump")
        var endReached = false
        process.inputStream
            .bufferedReader()
            .lines()
            .peek { log.info("log is: $it") }
            .forEach {
                val (isProgression, progression) = isProgressionLine(it)
                when {
                    isProgression -> broadcastProgression(downloadingInformation.item, progression)
                    isDownloadComplete(it) -> { endReached = true; finishDownload() }
                }
            }

        if (!endReached && !stopDownloading.get()) {
            throw RuntimeException("Unexpected ending, failed download")
        }
    }

    override fun startDownload() {
        stopDownloading.set(false)
        state.startDownload(this, this::failDownload)
    }

    override fun stopDownload() {
        stopDownloading.set(true)
        destroyProcess()
        state.stopDownload()
    }

    private fun destroyProcess() {
        if (p != null) p!!.destroy()
    }

    override fun failDownload() {
        stopDownloading.set(true)
        destroyProcess()
        state.failDownload()
    }

    override fun finishDownload() = state.finishDownload()

    private fun broadcastProgression(item: DownloadingItem, progression: Int) {
        if (item.progression != progression)
            state.broadcast(downloadingInformation)
    }

    private fun isProgressionLine(line: String): Pair<Boolean, Int> {
        val m = RTMP_DUMP_PROGRESSION.matcher(line)
        val isMatch = m.matches()
        return if (isMatch) true to m.group(1).toInt()
        else false to -1
    }

    private fun isDownloadComplete(line: String) = line.lowercase(Locale.getDefault()).contains(DOWNLOAD_COMPLETE)

    companion object {
        private const val DOWNLOAD_COMPLETE = "download complete"
        private val RTMP_DUMP_PROGRESSION = Pattern.compile("[^(]*\\(([0-9]*).*%\\)")!!
    }
}
