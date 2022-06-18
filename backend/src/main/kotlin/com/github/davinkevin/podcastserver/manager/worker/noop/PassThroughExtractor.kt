package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.lang.Integer.MAX_VALUE
import java.net.URI
import kotlin.io.path.Path

/**
 * Created by kevin on 03/12/2017
 */
@Component
@Scope("prototype")
class PassThroughExtractor : Extractor {

    override fun extract(item: DownloadingItem): DownloadingInformation {
        val fileName = Path(item.url.path).fileName
        return DownloadingInformation(item, listOf(item.url), fileName, null)
    }


    override fun compatibility(url: URI): Int = MAX_VALUE - 1

}
