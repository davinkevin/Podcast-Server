package com.github.davinkevin.podcastserver.update.updaters.upload

import com.github.davinkevin.podcastserver.manager.worker.ItemFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

class UploadUpdater : Updater {

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> = Flux.empty()
    override fun signatureOf(url: URI): Mono<String> = "".toMono()

    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> = TODO("not required anymore...")
    override fun blockingSignatureOf(url: URI): String = TODO("not required anymore...")

    override fun type(): Type = TYPE

    override fun compatibility(url: String?) = Integer.MAX_VALUE

    companion object {
        val TYPE = Type("upload", "Upload")
    }
}
