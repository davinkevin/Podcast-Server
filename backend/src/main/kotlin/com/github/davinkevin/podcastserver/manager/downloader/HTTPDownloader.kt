package com.github.davinkevin.podcastserver.manager.downloader

import com.github.axet.wget.info.DownloadInfo
import com.github.axet.wget.info.URLInfo.States.*
import com.github.axet.wget.info.ex.DownloadInterruptedError
import com.github.axet.wget.info.ex.DownloadMultipartError
import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.factory.WGetFactory
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Scope(SCOPE_PROTOTYPE)
@Component
class HTTPDownloader(
        downloadRepository: DownloadRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        clock: Clock,
        val urlService: UrlService,
        val wGetFactory: WGetFactory
) : AbstractDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock) {

    private val log = LoggerFactory.getLogger(HTTPDownloader::class.java)

    private val stopDownloading = AtomicBoolean(false)
    internal lateinit var info: DownloadInfo

    override fun download(): DownloadingItem {
        log.debug("Download")
        try {
            val url = downloadingInformation.url()
            val realUrl = urlService.getRealURL(url)

            info = wGetFactory.newDownloadInfo(realUrl)

            val itemSynchronisation = HTTPWatcher(this)

            info.extract(stopDownloading, itemSynchronisation)

            target = computeTargetFile(downloadingInformation)
            val w = wGetFactory.newWGet(info, target!!.toFile())

            w.download(stopDownloading, itemSynchronisation)
        } catch (e: DownloadMultipartError) {
            e.info.parts
                    .stream()
                    .map { it.exception }
                    .filter { it != null }
                    .forEach { it.printStackTrace() }

            throw RuntimeException(e)
        } catch (e: DownloadInterruptedError) {
            log.debug("Arrêt du téléchargement")
        }

        return downloadingInformation.item
    }

    override fun startDownload() {
        stopDownloading.set(false)
        super.startDownload()
    }

    override fun pauseDownload() {
        stopDownloading.set(true)
        super.pauseDownload()
    }

    override fun stopDownload() {
        stopDownloading.set(true)
        super.stopDownload()
    }

    override fun failDownload() {
        stopDownloading.set(true)
        super.failDownload()
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) =
            if (downloadingInformation.urls.size == 1 && downloadingInformation.urls.first().startsWith("http")) Integer.MAX_VALUE - 1
            else Integer.MAX_VALUE

    internal fun progression(progress: Int) {
        log.debug("Progression of {} : {}%", downloadingInformation.item.title, progress)
        downloadingInformation = downloadingInformation.progression(progress)
        broadcast(downloadingInformation.item)

    }
}

class HTTPWatcher(private val downloader: HTTPDownloader) : Runnable {

    val log = LoggerFactory.getLogger(HTTPWatcher::class.java)!!

    override fun run() {

        val id = downloader.downloadingInformation.item.id

        val info = downloader.info
        val itemDownloadManager = downloader.itemDownloadManager
        val dInfo = downloader.downloadingInformation
        val fileName = FilenameUtils.getName(downloader.downloadingInformation.url())

        when (info.state) {
            EXTRACTING, EXTRACTING_DONE -> log.debug("$fileName ${info.state}")
            DONE -> {
                log.debug("$fileName - download finished")
                downloader.finishDownload()
                itemDownloadManager.removeACurrentDownload(id)
            }
            RETRYING -> log.debug("$fileName ${info.state} ${info.delay}")
            DOWNLOADING -> {
                if (info.length == null || info.length == 0L) {
                    return
                }

                val progression = (info.count * 100 / info.length.toFloat()).toInt()
                if (dInfo.item.progression < progression) {
                    downloader.progression(progression)
                }
            }
            STOP -> log.debug("Pause / Stop of the download")
            else -> {}
        }
    }
}

