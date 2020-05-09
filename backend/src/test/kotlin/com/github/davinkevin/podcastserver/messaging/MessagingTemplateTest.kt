package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.test.StepVerifier
import java.net.URI
import java.util.*

/**
 * Created by kevin on 02/05/2020
 */
@ExtendWith(SpringExtension::class)
@Import(MessagingTemplate::class)
class MessagingTemplateTest(
        @Autowired private val messages: MessagingTemplate
) {

    private val item1 = DownloadingItem(
            id = UUID.randomUUID(),
            title = "Title 1",
            status = Status.NOT_DOWNLOADED,
            url = URI("https://foo.bar.com/podcast/title-1"),
            numberOfFail = 0,
            progression = 0,
            podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
            cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-1.jpg"))
    )
    private val item2 = DownloadingItem(
            id = UUID.randomUUID(),
            title = "Title 2",
            status = Status.STARTED,
            url = URI("https://foo.bar.com/podcast/title-2"),
            numberOfFail = 0,
            progression = 50,
            podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
            cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-2.jpg"))
    )
    private val item3 = DownloadingItem(
            id = UUID.randomUUID(),
            title = "Title 3",
            status = Status.STARTED,
            url = URI("https://foo.bar.com/podcast/title-3"),
            numberOfFail = 3,
            progression = 75,
            podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
            cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-3.jpg"))
    )
    private val item4 = DownloadingItem(
            id = UUID.randomUUID(),
            title = "Title 4",
            status = Status.FINISH,
            url = URI("https://foo.bar.com/podcast/title-4"),
            numberOfFail = 0,
            progression = 100,
            podcast = DownloadingItem.Podcast(UUID.randomUUID(), "podcast"),
            cover = DownloadingItem.Cover(UUID.randomUUID(), URI("https://foo.bar.com/podcast/title-4.jpg"))
    )

    @Test
    fun `should send waiting queue`() {
        /* Given */
        /* When */
        StepVerifier.create(messages.messages.take(4))
                /* Then */
                .expectSubscription()
                .then { messages.sendWaitingQueue(listOf(item1, item2, item3)) }
                .expectNextMatches { it is WaitingQueueMessage && it.value.containsAll(listOf(item1, item2, item3)) }
                .then { messages.sendWaitingQueue(listOf(item1, item3, item4)) }
                .expectNextMatches { it is WaitingQueueMessage && it.value.containsAll(listOf(item1, item3, item4)) }
                .then { messages.sendWaitingQueue(listOf(item1, item2)) }
                .expectNextMatches { it is WaitingQueueMessage && it.value.containsAll(listOf(item1, item2)) }
                .then { messages.sendWaitingQueue(listOf(item4)) }
                .expectNextMatches { it is WaitingQueueMessage && it.value.containsAll(listOf(item4)) }
                .verifyComplete()
    }

    @Test
    fun `should send downloading item`() {
        /* Given */
        /* When */
        StepVerifier.create(messages.messages.take(4))
                /* Then */
                .expectSubscription()
                .then { messages.sendItem(item1) }
                .expectNextMatches { it is DownloadingItemMessage && it.value == item1 }
                .then { messages.sendItem(item2) }
                .expectNextMatches { it is DownloadingItemMessage && it.value == item2 }
                .then { messages.sendItem(item3) }
                .expectNextMatches { it is DownloadingItemMessage && it.value == item3 }
                .then { messages.sendItem(item4) }
                .expectNextMatches { it is DownloadingItemMessage && it.value == item4 }
                .verifyComplete()
    }

    @Test
    fun `should update`() {
        /* Given */
        /* When */
        StepVerifier.create(messages.messages.take(3))
                /* Then */
                .expectSubscription()
                .then { messages.isUpdating(true) }
                .expectNextMatches { it is UpdateMessage && it.value }
                .then { messages.isUpdating(false) }
                .expectNextMatches { it is UpdateMessage && !it.value }
                .then { messages.isUpdating(true) }
                .expectNextMatches { it is UpdateMessage && it.value }
                .verifyComplete()
    }
}
