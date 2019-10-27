package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import org.apache.commons.io.FilenameUtils
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.lang.Integer.MAX_VALUE
import java.net.URI

/**
 * Created by kevin on 03/12/2017
 */
@Component
@Scope("prototype")
class PassThroughExtractor : Extractor {

    override fun extract(item: DownloadingItem): DownloadingInformation {
        val url = item.url.toASCIIString()
        val fileName = FilenameUtils.getName(url.substringBefore("?"))

        return DownloadingInformation(item, listOf(url), fileName, null)
    }


    override fun compatibility(url: URI): Int = MAX_VALUE - 1

}
