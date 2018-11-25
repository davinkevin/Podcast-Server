package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption
import com.github.axet.wget.info.DownloadInfo
import com.github.axet.wget.info.URLInfo.States.*
import com.github.axet.wget.info.ex.DownloadInterruptedError
import com.github.axet.wget.info.ex.DownloadMultipartError
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.factory.WGetFactory
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.entity.Item
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Scope(SCOPE_PROTOTYPE)
@Component
class HTTPDownloader(
        itemRepository: ItemRepository,
        podcastRepository: PodcastRepository,
        podcastServerParameters: PodcastServerParameters,
        template: SimpMessagingTemplate,
        mimeTypeService: MimeTypeService,
        val urlService: UrlService,
        val wGetFactory: WGetFactory
) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

    private val log = LoggerFactory.getLogger(HTTPDownloader::class.java)

    lateinit var info: DownloadInfo

    override fun download(): Item {
        log.debug("Download")
        try {
            info = this.downloadingItem.url()
                    .orElse { getItemUrl(item).toOption() }
                    .map { urlService.getRealURL(it) }
                    .map { wGetFactory.newDownloadInfo(it) }
                    .getOrElse { throw RuntimeException("Error during creation of download of ${downloadingItem.item.title}") }

            val itemSynchronisation = HTTPWatcher(this)

            info.extract(stopDownloading, itemSynchronisation)

            target = getTargetFile(item)
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

        return item
    }

    override fun compatibility(downloadingItem: DownloadingItem) =
            if (downloadingItem.urls.size == 1 && downloadingItem.urls.first().startsWith("http")) Integer.MAX_VALUE - 1
            else Integer.MAX_VALUE
}

class HTTPWatcher(private val httpDownloader: HTTPDownloader) : Runnable {

    val log = LoggerFactory.getLogger(HTTPWatcher::class.java)!!

    override fun run() {

        val info = httpDownloader.info
        val itemDownloadManager = httpDownloader.itemDownloadManager
        val item = httpDownloader.item

        when (info.state) {
            EXTRACTING, EXTRACTING_DONE -> log.debug(FilenameUtils.getName(httpDownloader.getItemUrl(item).toString()) + " " + info.state)
            DONE -> {
                log.debug(FilenameUtils.getName(httpDownloader.getItemUrl(item).toString()) + " - download finished")
                httpDownloader.finishDownload()
                itemDownloadManager.removeACurrentDownload(item)
            }
            RETRYING -> log.debug(FilenameUtils.getName(httpDownloader.getItemUrl(item).toString()) + " " + info.state + " " + info.delay)
            DOWNLOADING -> {
                if (info.length == null || info.length == 0L) {
                    return
                }

                val progression = (info.count * 100 / info.length.toFloat()).toInt()
                if (item.progression < progression) {
                    item.progression = progression
                    log.debug("Progression of {} : {}%", item.title, progression)
                    httpDownloader.convertAndSaveBroadcast()
                }
            }
            STOP -> log.debug("Pause / Stop of the download")
            else -> {}
        }
    }
}

