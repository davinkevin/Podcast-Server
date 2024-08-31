package com.github.davinkevin.podcastserver.messaging

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import org.awaitility.Awaitility.await
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
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by kevin on 02/05/2020
 */
@ExtendWith(SpringExtension::class)
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
}
