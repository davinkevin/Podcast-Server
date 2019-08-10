package com.github.davinkevin.podcastserver.manager.worker

import com.github.davinkevin.podcastserver.service.CoverInformation
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.ZonedDateTime
import java.util.*

val log = LoggerFactory.getLogger(Updater::class.java)!!
val defaultPodcast = PodcastToUpdate(id = UUID.randomUUID(), url = URI("https://localhost/"), signature = "")
val NO_MODIFICATION = UpdatePodcastInformation(defaultPodcast, setOf(), "")

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

    fun findItems(podcast: PodcastToUpdate): Set<ItemFromUpdate>

    fun signatureOf(url: URI): String

    fun type(): Type

    fun compatibility(url: String?): Int
}


class UpdatePodcastInformation(val podcast: PodcastToUpdate, val items: Set<ItemFromUpdate>, val newSignature: String) {
    operator fun component1() = podcast
    operator fun component2() = items
    operator fun component3() = newSignature
}
data class PodcastToUpdate(val id: UUID, val url: URI, val signature: String)
data class ItemFromUpdate(
        val title: String?,
        val pubDate: ZonedDateTime?,
        val length: Long? = null,
        val url: URI,
        val description: String?,
        val cover: CoverFromUpdate?
)
data class CoverFromUpdate(val width: Int, val height: Int, val url: URI)
fun CoverInformation.toCoverFromUpdate() = CoverFromUpdate (
    height = this@toCoverFromUpdate.height,
    width = this@toCoverFromUpdate.width,
    url = this@toCoverFromUpdate.url
)

val defaultItem = ItemFromUpdate(null, ZonedDateTime.now(), null, URI("http://foo.bar"), null, null)
