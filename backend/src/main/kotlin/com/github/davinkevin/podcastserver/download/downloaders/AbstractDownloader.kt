package com.github.davinkevin.podcastserver.download.downloaders


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Clock

abstract class AbstractDownloader(
    private val downloadRepository: DownloadRepository,
    private val template: MessagingTemplate,
    clock: Clock,
    file: FileStorageService,
) : Downloader {

    private val log = LoggerFactory.getLogger(AbstractDownloader::class.java)
    private val helperFactory: DownloaderHelperFactory = DownloaderHelperFactory(downloadRepository, template, clock, file)

    internal lateinit var helper: DownloaderHelper
    internal open lateinit var itemDownloadManager: ItemDownloadManager

    override var downloadingInformation: DownloadingInformation
        get() = helper.item
        set(value) {
            helper.item = value
        }

    var target: Path
        get() = helper.target
        set(value) {
            helper.target = value
        }

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        this.helper = helperFactory.build(information, itemDownloadManager)
        this.downloadingInformation = information
        this.itemDownloadManager = itemDownloadManager
        return this
    }

    override fun run() {
        log.info("Starting download of ${downloadingInformation.item.url}")
        startDownload()
    }

    override fun startDownload() {
        helper.startDownload(this::download)
    }

    override fun stopDownload() {
        helper.stopDownload()
    }

    override fun failDownload() {
        helper.failDownload()
    }

    override fun finishDownload() {
        helper.finishDownload()
    }

    internal fun computeTargetFile(info: DownloadingInformation): Path = helper.computeTargetFile(info)

    internal fun saveStateOfItem(info: DownloadingInformation) {
        helper.saveState(info)
    }

    internal fun broadcast(info: DownloadingInformation) {
        helper.broadcast(info)
    }
}