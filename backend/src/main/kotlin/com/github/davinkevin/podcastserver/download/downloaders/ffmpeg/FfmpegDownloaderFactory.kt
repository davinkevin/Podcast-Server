package com.github.davinkevin.podcastserver.download.downloaders.ffmpeg

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.rtmp.RTMPDownloader
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import java.util.*

class FfmpegDownloaderFactory(
    private val helperFactory: DownloaderHelperFactory,
    private val ffmpegService: FfmpegService,
    private val processService: ProcessService,
): DownloaderFactory {

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        return FfmpegDownloader(helperFactory.build(information, itemDownloadManager), ffmpegService, processService)
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) =
            if (downloadingInformation.urls.map { it.toASCIIString().lowercase(Locale.getDefault()) }.all { "m3u8" in it || "mp4" in it }) 10
            else Integer.MAX_VALUE
}