package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by kevin on 2018-11-25
 *
 * Two distinct concerns are signalled here, deliberately decoupled:
 *
 *   - `isUpdating(boolean)` flags the bulk refresh — emitted exclusively
 *     at the boundaries of `UpdateService.updateAll`. Toggles true at the
 *     start of the bulk and false at the end.
 *   - `isPodcastUpdating(id, boolean)` flags a single-podcast refresh,
 *     emitted around `UpdateService.update`. Carries the podcast id so the
 *     UI can show a spinner on the exact "Update now" button that fired it.
 *
 * Keeping them independent avoids the race a coupled design would create
 * (multiple single-podcast runs flipping a shared global flag off while
 * another one is still in flight).
 */
class MessagingTemplate(
    private val event: ApplicationEventPublisher
) {
    // All events are published through a single-threaded executor so their
    // relative order is preserved: a podcast's `updating:true` always reaches
    // subscribers before its `updating:false`. A thread-per-event design (the
    // previous `Thread.ofVirtual().start { ... }`) raced these two apart, and
    // when `false` overtook `true` the UI left the "Update now" spinner stuck
    // on for good. Publishing off the caller thread keeps emission
    // non-blocking; the single worker also serializes access to the downstream
    // `Sinks.Many`, which is not safe for concurrent emission. The worker is a
    // virtual (daemon) thread, so it never holds up JVM shutdown.
    private val publisher: ExecutorService =
        Executors.newSingleThreadExecutor(Thread.ofVirtual().factory())

    fun sendWaitingQueue(value: List<DownloadingItem>) {
        val v = WaitingQueueMessage(value)
        publisher.execute { event.publishEvent(v) }
    }
    fun sendItem(value: DownloadingItem) {
        val v = DownloadingItemMessage(value)
        publisher.execute { event.publishEvent(v) }
    }
    fun isUpdating(value: Boolean) {
        val v = UpdateMessage(value)
        publisher.execute { event.publishEvent(v) }
    }
    fun isPodcastUpdating(podcastId: UUID, updating: Boolean) {
        val v = PodcastUpdatingMessage(PodcastUpdatingValue(podcastId, updating))
        publisher.execute { event.publishEvent(v) }
    }
}

sealed class Message<T>(val topic: String, val value: T)
class UpdateMessage(value: Boolean): Message<Boolean>("updating", value)
class WaitingQueueMessage(value: List<DownloadingItem>): Message<List<DownloadingItem>>("waiting", value)
class DownloadingItemMessage(value: DownloadingItem): Message<DownloadingItem>("downloading", value)
class PodcastUpdatingMessage(value: PodcastUpdatingValue): Message<PodcastUpdatingValue>("podcast-updating", value)

data class PodcastUpdatingValue(val podcastId: UUID, val updating: Boolean)
