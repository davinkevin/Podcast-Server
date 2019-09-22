package com.github.davinkevin.podcastserver.manager.worker

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.apache.commons.io.FilenameUtils
import java.net.URI

/**
 * Created by kevin on 03/12/2017
 */
interface Extractor {

    fun extract(item: DownloadingItem): DownloadingInformation
    fun compatibility(url: URI): Int

    fun getFileName(url: URI): String {
        val urlFile =  url.toASCIIString().substringBefore("?")
        return FilenameUtils.getName(urlFile)
    }
}
