package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

internal fun ImageService.fetchCoverInformationOrOption(url: URI) =
        this.fetchCoverInformation(url)
                .map { FindCoverInformation(it.height, it.width, it.url) }
                .map { Optional.of(it) }
                .switchIfEmpty { Optional.empty<FindCoverInformation>().toMono() }

internal fun ImageService.fetchFindCoverInformation(url: URI): FindCoverInformation? {
        val info = this.fetchCoverInformation(url).block()
                ?: return null

        return FindCoverInformation(info.height, info.width, info.url)
}