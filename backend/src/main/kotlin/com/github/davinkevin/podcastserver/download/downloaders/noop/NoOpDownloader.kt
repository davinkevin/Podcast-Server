package com.github.davinkevin.podcastserver.download.downloaders.noop

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem


class NoOpDownloaderFactory: DownloaderFactory {
    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        return NoOpDownloader(information, itemDownloadManager)
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) = -1
}

class NoOpDownloader(
    val state: DownloadingInformation,
    val manager: ItemDownloadManager,
) : Downloader {

    //* To be removed when Downloader won't be anymore a DownloaderFactory *//
    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader =
        throw IllegalAccessException()
    override fun compatibility(downloadingInformation: DownloadingInformation) = -1
    override val downloadingInformation: DownloadingInformation
        get() = state
    //* end of section to remove *//

    override fun download(): DownloadingItem = state.item
    override fun startDownload() = failDownload()
    override fun stopDownload() {}
    override fun finishDownload() {}
    override fun failDownload() = manager.removeACurrentDownload(state.item.id)
}
