package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import java.util.*

class YoutubeDlDownloaderFactory(
    private val stateFactory: DownloaderHelperFactory,
    private val youtubeDL: YoutubeDlService,
) : DownloaderFactory {

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): YoutubeDlDownloader {
        return YoutubeDlDownloader(stateFactory.build(information, itemDownloadManager), youtubeDL)
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