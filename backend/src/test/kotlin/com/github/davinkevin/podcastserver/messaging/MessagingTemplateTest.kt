package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Created by kevin on 02/05/2020
 */
@NestedSpringTest
class MessagingTemplateTest(
        @Autowired private val messages: MessagingTemplate,
        @Autowired private val event: ApplicationEventPublisher,
) {

    @TestConfiguration
    @Import(MessagingTemplate::class)
    class TestConfig {
        @Bean
        @Primary
        fun event(): ApplicationEventPublisher = mock()
    }

    // Shared event mock across tests via the Spring context — reset
    // between tests so `verify(times(N))` counts start at 0 per test
    // method.
    @BeforeEach
    fun resetEventMock() {
        reset(event)
    }

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
        doNothing().whenever(event).publishEvent(any<WaitingQueueMessage>())

        /* When */
        messages.sendWaitingQueue(listOf(item1, item2, item3))

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<WaitingQueueMessage>()
            verify(event).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.firstValue.topic).isEqualTo("waiting")
                assertThat(captor.firstValue.value).isEqualTo(listOf(item1, item2, item3))
            }
        }

        /* When */
        messages.sendWaitingQueue(listOf(item1, item3, item4))

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<WaitingQueueMessage>()
            verify(event, times(2)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.secondValue.topic).isEqualTo("waiting")
                assertThat(captor.secondValue.value).isEqualTo(listOf(item1, item3, item4))
            }
        }

        /* When */
        messages.sendWaitingQueue(listOf(item1, item2))

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<WaitingQueueMessage>()
            verify(event, times(3)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.thirdValue.topic).isEqualTo("waiting")
                assertThat(captor.thirdValue.value).isEqualTo(listOf(item1, item2))
            }
        }

        /* When */
        messages.sendWaitingQueue(listOf(item4))

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<WaitingQueueMessage>()
            verify(event, times(4)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.allValues[3].topic).isEqualTo("waiting")
                assertThat(captor.allValues[3].value).isEqualTo(listOf(item4))
            }
        }
    }

    @Test
    fun `should send downloading item`() {
        /* Given */
        doNothing().whenever(event).publishEvent(any<DownloadingItemMessage>())

        /* When */
        messages.sendItem(item1)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<DownloadingItemMessage>()
            verify(event).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.firstValue.topic).isEqualTo("downloading")
                assertThat(captor.firstValue.value).isEqualTo(item1)
            }
        }

        /* When */
        messages.sendItem(item2)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<DownloadingItemMessage>()
            verify(event, times(2)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.secondValue.topic).isEqualTo("downloading")
                assertThat(captor.secondValue.value).isEqualTo(item2)
            }
        }

        /* When */
        messages.sendItem(item3)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<DownloadingItemMessage>()
            verify(event, times(3)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.thirdValue.topic).isEqualTo("downloading")
                assertThat(captor.thirdValue.value).isEqualTo(item3)
            }
        }

        /* When */
        messages.sendItem(item4)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<DownloadingItemMessage>()
            verify(event, times(4)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.allValues[3].topic).isEqualTo("downloading")
                assertThat(captor.allValues[3].value).isEqualTo(item4)
            }
        }
    }

    @Test
    fun `should update`() {
        /* Given */
        doNothing().whenever(event).publishEvent(any<UpdateMessage>())

        /* When */
        messages.isUpdating(true)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<UpdateMessage>()
            verify(event).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.firstValue.topic).isEqualTo("updating")
                assertThat(captor.firstValue.value).isEqualTo(true)
            }
        }

        /* When */
        messages.isUpdating(false)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<UpdateMessage>()
            verify(event, times(2)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.secondValue.topic).isEqualTo("updating")
                assertThat(captor.secondValue.value).isEqualTo(false)
            }
        }

        /* When */
        messages.isUpdating(true)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val captor = argumentCaptor<UpdateMessage>()
            verify(event, times(3)).publishEvent(captor.capture())

            assertAll {
                assertThat(captor.thirdValue.topic).isEqualTo("updating")
                assertThat(captor.thirdValue.value).isEqualTo(true)
            }
        }
    }

    @Test
    fun `should emit per-podcast updating without touching the global flag`() {
        /* Given */
        doNothing().whenever(event).publishEvent(any<PodcastUpdatingMessage>())
        val podcastId = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66")

        /* When */
        messages.isPodcastUpdating(podcastId, true)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val podcast = argumentCaptor<PodcastUpdatingMessage>()
            verify(event).publishEvent(podcast.capture())

            assertAll {
                assertThat(podcast.firstValue.topic).isEqualTo("podcast-updating")
                assertThat(podcast.firstValue.value)
                    .isEqualTo(PodcastUpdatingValue(podcastId, true))
            }
        }

        /* When */
        messages.isPodcastUpdating(podcastId, false)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val podcast = argumentCaptor<PodcastUpdatingMessage>()
            verify(event, times(2)).publishEvent(podcast.capture())

            assertAll {
                assertThat(podcast.secondValue.value)
                    .isEqualTo(PodcastUpdatingValue(podcastId, false))
            }
        }

        // Per-podcast events are completely decoupled from the global
        // `isUpdating` flag — they should never publish an UpdateMessage.
        // This guards against accidental coupling reintroducing the race
        // around concurrent single-podcast updates flipping the global
        // false while another is still running.
        verify(event, never()).publishEvent(any<UpdateMessage>())
    }

    @Test
    fun `should preserve order between successive per-podcast updating events`() {
        /* Given */
        // Records the `updating` flag of each event in the order the publisher
        // actually receives it. Handling `true` slowly means that if the two
        // emissions are NOT serialized, the later `false` overtakes it — the
        // exact reordering that leaves the "Update now" spinner stuck on.
        val received = CopyOnWriteArrayList<Boolean>()
        doAnswer { invocation ->
            val message = invocation.getArgument<PodcastUpdatingMessage>(0)
            if (message.value.updating) Thread.sleep(300)
            received.add(message.value.updating)
            null
        }.whenever(event).publishEvent(any<PodcastUpdatingMessage>())
        val podcastId = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66")

        /* When */
        // Fired back-to-back, mirroring UpdateService.update: `true` at the
        // start of the run, `false` at the end.
        messages.isPodcastUpdating(podcastId, true)
        messages.isPodcastUpdating(podcastId, false)

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertAll {
                assertThat(received).containsExactly(true, false)
            }
        }
    }
}
