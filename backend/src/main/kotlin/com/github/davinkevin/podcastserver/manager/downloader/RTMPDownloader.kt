package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import lan.dk.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Status
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.io.File
import java.util.regex.Pattern

@Component
@Scope(SCOPE_PROTOTYPE)
class RTMPDownloader(
        itemRepository: ItemRepository,
        podcastRepository: PodcastRepository,
        podcastServerParameters: PodcastServerParameters,
        template: SimpMessagingTemplate,
        mimeTypeService: MimeTypeService,
        val processService: ProcessService,
        val externalTools: ExternalTools
) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

    private val log = LoggerFactory.getLogger(RTMPDownloader::class.java)

    internal var pid = 0
    private var p: Process? = null

    override fun download(): Item {
        log.debug("Download")

        try {
            target = getTargetFile(item)
            log.debug("out file : {}", target!!.toAbsolutePath().toString())

            val processToExecute = processService
                    .newProcessBuilder(externalTools.rtmpdump, "-r", getItemUrl(item), "-o", target!!.toAbsolutePath().toString())
                    .directory(File("/tmp"))
                    .redirectErrorStream(true)

            p = processService.start(processToExecute)
            pid = processService.pidOf(p!!)

            listenLogs(p!!, item)

            p!!.waitFor()
            pid = 0
        } catch (e: Exception) { throw RuntimeException(e) }

        return item
    }

    fun listenLogs(process: Process, i: Item) {
        log.debug("Reading output of RTMPDump")

        process.inputStream
                .bufferedReader()
                .lines()
                .forEach {
                    val (isProgression, progression) = isProgressionLine(it)
                    if (isProgression) {
                        broadcastProgression(i, progression)
                        return@forEach
                    }

                    if (isDownloadComplete(it)) {
                        log.info("End of download")
                        finishDownload()
                        return@forEach
                    }
                }

        if (i.status != Status.FINISH && !stopDownloading.get()) {
            throw RuntimeException("Unexpected ending, failed download")
        }
    }

    override fun pauseDownload() =
            try {
                val stopProcess = processService.newProcessBuilder("kill", "-STOP", pid.toString())
                processService.start(stopProcess)
                super.pauseDownload()
            } catch (e: Exception) {
                log.error("IOException :", e)
                failDownload()
            }

    override fun restartDownload() =
            try {
                val restart = processService.newProcessBuilder("kill", "-SIGCONT", "" + pid.toString())
                processService.start(restart)
                item.status = Status.STARTED
                saveSyncWithPodcast()
                convertAndSaveBroadcast()
            } catch (e: Exception) {
                log.error("Error during restart of process :", e)
                failDownload()
            }

    override fun stopDownload() {
        destroyProcess()
        super.stopDownload()
    }

    private fun destroyProcess() {
        if (p != null) p!!.destroy()
    }

    override fun failDownload() {
        destroyProcess()
        super.failDownload()
    }

    override fun compatibility(downloadingItem: DownloadingItem) =
            if (downloadingItem.urls.size == 1 && downloadingItem.urls.first().startsWith("rtmp://")) 1
            else Integer.MAX_VALUE

    private fun broadcastProgression(item: Item, progression: Int) {
        if (item.progression != progression)
            convertAndSaveBroadcast()
    }

    private fun isProgressionLine(line: String): Pair<Boolean, Int> {
        val m = RTMP_DUMP_PROGRESSION.matcher(line)
        val isMatch = m.matches()
        return if(isMatch) true to m.group(1).toInt()
        else false to -1
    }

    private fun isDownloadComplete(line: String) = line.toLowerCase().contains(DOWNLOAD_COMPLETE)

    companion object {
        private const val DOWNLOAD_COMPLETE = "download complete"
        private val RTMP_DUMP_PROGRESSION = Pattern.compile("[^(]*\\(([0-9]*).*%\\)")!!
    }
}
