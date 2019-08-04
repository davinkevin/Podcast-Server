package com.github.davinkevin.podcastserver.manager.worker

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.util.*

val log = LoggerFactory.getLogger(Updater::class.java)!!
val NO_MODIFICATION = UpdatePodcastInformation(Podcast.DEFAULT_PODCAST, setOf(), null)

interface Updater {


    fun update(podcast: PodcastToUpdate): UpdatePodcastInformation {
        return try {
            val signature = signatureOf(podcast.url)
            if (signature == podcast.signature) {
                log.info(""""{}" hasn't change""", podcast.url)
                return NO_MODIFICATION
            }
            UpdatePodcastInformation(podcast, findItems(podcast), signature)
        } catch (e: Exception) {
            log.info("""podcast with id "{}" and url {} triggered the following error during update""", podcast.id, podcast.url, e)
            NO_MODIFICATION
        }
    }

    fun findItems(podcast: PodcastToUpdate): Set<Item>

    fun signatureOf(url: URI): String

    fun type(): Type

    fun compatibility(url: String?): Int
}


class UpdatePodcastInformation(val podcast: PodcastToUpdate, val items: Set<Item>, val newSignature: String?)
class PodcastToUpdate(val id: UUID, val url: URI, val signature: String)
