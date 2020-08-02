package com.github.davinkevin.podcastserver.update.updaters.upload

import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

class UploadUpdater : Updater {

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> = Flux.empty()
    override fun signatureOf(url: URI): Mono<String> = "".toMono()
    override fun type(): Type = TYPE
    override fun compatibility(url: String): Int = Integer.MAX_VALUE
}

private val TYPE = Type("upload", "Upload")
