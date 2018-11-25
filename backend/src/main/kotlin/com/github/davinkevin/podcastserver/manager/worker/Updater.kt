package com.github.davinkevin.podcastserver.manager.worker

import lan.dk.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import org.slf4j.LoggerFactory

interface Updater {

    fun update(podcast: Podcast): UpdatePodcastInformation {
        return try {
            val signature = signatureOf(podcast)
            if (signature == podcast.signature) {
                log.info(""""{}" hasn't change""", podcast.title)
                return NO_MODIFICATION
            }
            podcast.signature = signature
            UpdatePodcastInformation(podcast, findItems(podcast), notIn(podcast))
        } catch (e: Exception) {
            log.info(""""{}" triggered the following error during update""", podcast.title, e)
            NO_MODIFICATION
        }
    }

    fun findItems(podcast: Podcast): Set<Item>

    fun signatureOf(podcast: Podcast): String

    fun notIn(podcast: Podcast): (Item) -> Boolean = { item -> !podcast.contains(item) }

    fun type(): Type

    fun compatibility(url: String?): Int

    companion object {
        val log = LoggerFactory.getLogger(Updater::class.java)
        val NO_MODIFICATION = UpdatePodcastInformation(Podcast.DEFAULT_PODCAST, setOf()) { true }
    }
}


class UpdatePodcastInformation(val podcast: Podcast, val items: Set<Item>, val p: (Item) -> Boolean)