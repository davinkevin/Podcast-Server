package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.find.FindCoverInformation
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

internal fun ImageService.fetchCoverInformationOrOption(url: URI) =
        this.fetchCoverInformation(url)
                .map { FindCoverInformation(it.height, it.width, it.url) }
                .map { Optional.of<FindCoverInformation?>(it) }
                .switchIfEmpty { Optional.empty<FindCoverInformation>().toMono() }
