package com.github.davinkevin.podcastserver.download.downloaders.rtmp

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools

class RTMPDownloaderFactory(
    private val helperFactory: DownloaderHelperFactory,
    private val processService: ProcessService,
    private val externalTools: ExternalTools,
): DownloaderFactory {

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        return RTMPDownloader(helperFactory.build(information, itemDownloadManager), processService, externalTools)
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) =
        if (downloadingInformation.urls.size == 1 && downloadingInformation.urls.first().toASCIIString().startsWith("rtmp://")) 1
        else Integer.MAX_VALUE

}