package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.jsoup.nodes.Document
import java.net.URI

internal fun ImageService.fetchFindCoverInformation(url: URI): FindCoverInformation? {
        val info = fetchCoverInformation(url).block()
                ?: return null

        return FindCoverInformation(info.height, info.width, info.url)
}

internal fun Document.meta(s: String) = this.select("meta[$s]").attr("content")