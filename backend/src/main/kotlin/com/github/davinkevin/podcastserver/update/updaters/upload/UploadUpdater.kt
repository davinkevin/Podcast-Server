package com.github.davinkevin.podcastserver.update.updaters.upload

import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import io.micrometer.core.instrument.MeterRegistry
import java.net.URI

class UploadUpdater(
    override val registry: MeterRegistry,
) : Updater {
    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> = emptyList()
    override fun signatureOf(url: URI): String = ""
    override fun type(): Type = TYPE
    override fun compatibility(url: String): Int = Integer.MAX_VALUE
}

private val TYPE = Type("upload", "Upload")
