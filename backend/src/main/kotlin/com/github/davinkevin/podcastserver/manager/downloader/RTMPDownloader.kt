package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.time.Clock
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

@Component
@Scope(SCOPE_PROTOTYPE)
class RTMPDownloader(
    downloadRepository: DownloadRepository,
    template: MessagingTemplate,
    clock: Clock,
    file: FileStorageService,
    val processService: ProcessService,
    val externalTools: ExternalTools
) : AbstractDownloader(downloadRepository, template, clock, file) {

    private val log = LoggerFactory.getLogger(RTMPDownloader::class.java)

    internal var pid = 0L
    private var p: Process? = null
    private val stopDownloading = AtomicBoolean(false)

    override fun download(): DownloadingItem {
        log.debug("Download")

        try {
            target = computeTargetFile(downloadingInformation)
            log.debug("out file : {}", target.toAbsolutePath().toString())

            val processToExecute = processService
                .newProcessBuilder(
                    externalTools.rtmpdump,
                    "-r",
                    downloadingInformation.url(),
                    "-o",
                    target.toAbsolutePath().toString()
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

        process.inputStream
            .bufferedReader()
            .lines()
            .forEach {
                val (isProgression, progression) = isProgressionLine(it)
                if (isProgression) {
                    broadcastProgression(downloadingInformation.item, progression)
                    return@forEach
                }

                if (isDownloadComplete(it)) {
                    log.info("End of download")
                    finishDownload()
                    return@forEach
                }
            }

        if (downloadingInformation.item.status != Status.FINISH && !stopDownloading.get()) {
            throw RuntimeException("Unexpected ending, failed download")
        }
    }

    override fun startDownload() {
        stopDownloading.set(false)
        super.startDownload()
    }

    override fun stopDownload() {
        stopDownloading.set(true)
        destroyProcess()
        super.stopDownload()
    }

    private fun destroyProcess() {
        if (p != null) p!!.destroy()
    }

    override fun failDownload() {
        stopDownloading.set(true)
        destroyProcess()
        super.failDownload()
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) =
        if (downloadingInformation.urls.size == 1 && downloadingInformation.urls.first().startsWith("rtmp://")) 1
        else Integer.MAX_VALUE

    private fun broadcastProgression(item: DownloadingItem, progression: Int) {
        if (item.progression != progression)
            broadcast(downloadingInformation.item)
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
