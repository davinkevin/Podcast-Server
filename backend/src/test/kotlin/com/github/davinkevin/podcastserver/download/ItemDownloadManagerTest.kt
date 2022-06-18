package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.io.path.Path

/**
 * Created by kevin on 06/05/15
 */
@ExtendWith(SpringExtension::class)
class ItemDownloadManagerTest(
    @Autowired val downloadExecutor: ThreadPoolTaskExecutor,
    @Autowired val idm: ItemDownloadManager
) {

    @MockBean private lateinit var messaging: MessagingTemplate
    @MockBean private lateinit var repository: DownloadRepository
    @MockBean private lateinit var parameters: PodcastServerParameters
    @MockBean private lateinit var downloaders: DownloaderSelector

    private val date = OffsetDateTime.of(2012, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @AfterEach
    fun afterEach() {
        Mockito.reset(messaging, repository, downloadExecutor)
        downloadExecutor.corePoolSize = 1
    }

    @Nested
    @DisplayName("should use limitParallelDownload")
    inner class ShouldUseLimitParallelDownload {

        @Test
        fun `and change its value`() {
            /* Given */
            whenever(repository.initQueue(date, 1))
                .thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(3)).thenReturn(Flux.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())

            /* When */
            idm.limitParallelDownload = 3

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                verify(messaging).sendWaitingQueue(emptyList())
            }
            assertThat(downloadExecutor.corePoolSize).isEqualTo(3)
        }

        @Test
        fun `and reflects value from internal executor`() {
            /* Given */
            /* When */
            /* Then */
            assertThat(idm.limitParallelDownload).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("should expose waiting queue")
    inner class ShouldExposeWaitingQueue {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        private val item2 = DownloadingItem(
            id = UUID.fromString("27455c79-3349-4359-aca9-d4ea2cedc538"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("493b2fa5-8dc3-4d66-8bce-556e60c9a173"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("5be6d50d-8288-47e3-84fe-7eb9e2184466"),
                title = "bar"
            )
        )

        @Test
        fun `with no item`() {
            /* Given */
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(idm.queue)
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with items`() {
            /* Given */
            whenever(repository.findAllWaiting()).thenReturn(Flux.just(item1, item2))
            /* When */
            StepVerifier.create(idm.queue)
                /* Then */
                .expectSubscription()
                .expectNext(item1)
                .expectNext(item2)
                .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should expose downloading queue")
    inner class ShouldExposeDownloadingQueue {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        private val item2 = DownloadingItem(
            id = UUID.fromString("27455c79-3349-4359-aca9-d4ea2cedc538"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("493b2fa5-8dc3-4d66-8bce-556e60c9a173"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("5be6d50d-8288-47e3-84fe-7eb9e2184466"),
                title = "bar"
            )
        )

        @Test
        fun `with no item`() {
            /* Given */
            whenever(repository.findAllDownloading()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(idm.downloading)
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with items`() {
            /* Given */
            whenever(repository.findAllDownloading()).thenReturn(Flux.just(item1, item2))
            /* When */
            StepVerifier.create(idm.downloading)
                /* Then */
                .expectSubscription()
                .expectNext(item1)
                .expectNext(item2)
                .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should launch download")
    inner class ShouldLaunchDownload {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        private val item2 = DownloadingItem(
            id = UUID.fromString("27455c79-3349-4359-aca9-d4ea2cedc538"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("493b2fa5-8dc3-4d66-8bce-556e60c9a173"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("5be6d50d-8288-47e3-84fe-7eb9e2184466"),
                title = "bar"
            )
        )

        @Test
        fun `with no item to download`() {
            /* Given */
            whenever(parameters.limitDownloadDate()).thenReturn(date)
            whenever(parameters.numberOfTry).thenReturn(10)
            whenever(repository.initQueue(date, 10)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(any())).thenReturn(Flux.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())

            /* When */
            StepVerifier.create(idm.launchDownload())
                /* Then */
                .expectSubscription()
                .verifyComplete()

            verify(repository, times(1)).initQueue(date, 10)
            verify(repository, times(1)).findAllToDownload(1)
            verify(messaging, times(1)).sendWaitingQueue(emptyList())

            verify(repository, never()).startItem(any())
            verify(downloadExecutor, never()).execute(any())
        }

        @Test
        fun `with one item`() {
            /* Given */
            val downloader = SimpleDownloader()
            val information = item1.toInformation()
            whenever(parameters.limitDownloadDate()).thenReturn(date)
            whenever(parameters.numberOfTry).thenReturn(10)
            whenever(repository.initQueue(date, 10)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(any())).thenReturn(Flux.just(item1))
            whenever(repository.findAllWaiting()).thenReturn(Flux.just(item1))
            whenever(downloaders.of(information)).thenReturn(downloader)
            whenever(repository.startItem(item1.id)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(idm.launchDownload())
                /* Then */
                .expectSubscription()
                .verifyComplete()

            verify(repository, times(1)).initQueue(date, 10)
            verify(repository, times(1)).findAllToDownload(1)
            verify(messaging, atLeast(1)).sendWaitingQueue(listOf(item1))

            assertThat(downloader.itemDownloadManager).isEqualTo(idm)
            assertThat(downloader.downloadingInformation).isEqualTo(information)
            verify(downloadExecutor, times(1)).execute(downloader)
        }

        @Test
        fun `with multiple items`() {
            /* Given */
            val downloader1 = SimpleDownloader()
            val information1 = item1.toInformation()
            val downloader2 = SimpleDownloader()
            val information2 = item2.toInformation()
            whenever(parameters.limitDownloadDate()).thenReturn(date)
            whenever(parameters.numberOfTry).thenReturn(10)
            whenever(repository.initQueue(date, 10)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(any())).thenReturn(Flux.just(item1, item2))
            whenever(repository.findAllWaiting()).thenReturn(Flux.just(item1, item2))
            whenever(downloaders.of(information1)).thenReturn(downloader1)
            whenever(downloaders.of(information2)).thenReturn(downloader2)

            whenever(repository.startItem(item1.id)).thenReturn(Mono.empty())
            whenever(repository.startItem(item2.id)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(idm.launchDownload())
                /* Then */
                .expectSubscription()
                .verifyComplete()

            verify(repository, times(1)).initQueue(date, 10)
            verify(repository, times(1)).findAllToDownload(1)
            verify(messaging, atLeast(1)).sendWaitingQueue(listOf(item1, item2))

            assertThat(downloader1.itemDownloadManager).isEqualTo(idm)
            assertThat(downloader1.downloadingInformation).isEqualTo(information1)
            assertThat(downloader2.itemDownloadManager).isEqualTo(idm)
            assertThat(downloader2.downloadingInformation).isEqualTo(information2)

            verify(downloadExecutor, times(1)).execute(downloader1)
            verify(downloadExecutor, times(1)).execute(downloader2)
        }

    }

    @Nested
    @DisplayName("should stop all download")
    inner class ShouldStopAllDownload {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        private val item2 = DownloadingItem(
            id = UUID.fromString("27455c79-3349-4359-aca9-d4ea2cedc538"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("493b2fa5-8dc3-4d66-8bce-556e60c9a173"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("5be6d50d-8288-47e3-84fe-7eb9e2184466"),
                title = "bar"
            )
        )

        @Test
        fun `with no items`() {
            /* Given */
            /* When */
            idm.stopAllDownload()
            /* Then */
            // ? nothing to test, because nothing was executed…
        }

        @Test
        fun `with multiple items`() {
            /* Given */
            val downloader1: SimpleDownloader = mock(spiedInstance = SimpleDownloader())
            val information1 = item1.toInformation()
            val downloader2: SimpleDownloader = mock(spiedInstance = SimpleDownloader())
            val information2 = item2.toInformation()
            whenever(parameters.limitDownloadDate()).thenReturn(date)
            whenever(parameters.numberOfTry).thenReturn(10)
            whenever(repository.initQueue(date, 10)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(any())).thenReturn(Flux.just(item1, item2))
            whenever(repository.findAllWaiting()).thenReturn(Flux.just(item1, item2))
            whenever(downloaders.of(information1)).thenReturn(downloader1)
            whenever(downloaders.of(information2)).thenReturn(downloader2)
            whenever(repository.startItem(item1.id)).thenReturn(Mono.empty())
            whenever(repository.startItem(item2.id)).thenReturn(Mono.empty())
            whenever(downloader1.with(any(), any())).thenCallRealMethod()
            whenever(downloader2.with(any(), any())).thenCallRealMethod()
            idm.launchDownload().block()

            /* When */
            idm.stopAllDownload()

            /* Then */
            verify(downloader1, times(1)).stopDownload()
            verify(downloader2, times(1)).stopDownload()
        }

    }

    @Nested
    @DisplayName("should add item to queue")
    inner class ShouldAddItemToQueue {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        @Test
        fun `with success`() {
            /* Given */
            whenever(repository.addItemToQueue(item1.id)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(1)).thenReturn(Flux.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(idm.addItemToQueue(item1.id))
                /* Then */
                .expectSubscription()
                .verifyComplete()

            verify(repository, times(1)).addItemToQueue(item1.id)
        }
    }

    @Nested
    @DisplayName("should remove item from queue")
    inner class ShouldRemoveItemFromQueue {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        @Test
        fun `with success`() {
            /* Given */
            whenever(repository.remove(item1.id, true)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(1)).thenReturn(Flux.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())

            /* When */
            idm.removeItemFromQueue(item1.id, true)

            /* Then */
            verify(repository, times(1)).remove(item1.id, true)
        }
    }

    @Nested
    @DisplayName("should remove item from downloading")
    inner class ShouldRemoveItemFromDownloading {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        @Test
        fun `with success`() {
            /* Given */
            whenever(repository.remove(item1.id, false)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(1)).thenReturn(Flux.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())

            /* When */
            idm.removeACurrentDownload(item1.id)

            /* Then */
            verify(repository, times(1)).remove(item1.id, false)
        }
    }

    @Nested
    @DisplayName("should remove item from queue and downloading")
    inner class ShouldRemoveItemFromQueueAndDownloading {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        @Test
        fun `with item only in waiting list`() {
            /* Given */
            whenever(repository.remove(item1.id, false)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(1)).thenReturn(Flux.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())

            /* When */
            StepVerifier.create(idm.removeItemFromQueueAndDownload(item1.id))
                /* Then */
                .expectSubscription()
                .verifyComplete()

            verify(repository, times(1)).remove(item1.id, false)
        }

        @Test
        fun `with item only in downloading list`() {
            /* Given */
            val downloader = SimpleDownloader()
            whenever(downloaders.of(any())).thenReturn(downloader)
            whenever(repository.addItemToQueue(item1.id)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(1)).thenReturn(Flux.just(item1))
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())
            whenever(repository.startItem(item1.id)).thenReturn(Mono.empty())
            idm.addItemToQueue(item1.id).block()

            /* When */
            StepVerifier.create(idm.removeItemFromQueueAndDownload(item1.id))
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should provide if is in downloading queue")
    inner class ShouldProvideIfIsInDownloadingQueue {

        private val item1 = DownloadingItem(
            id = UUID.fromString("1f8d2177-357e-4db3-82ff-65012b5ffc23"),
            title = "first",
            url = URI("https://foo.bar.com/1/url/com.mp3"),
            status = Status.NOT_DOWNLOADED,
            numberOfFail = 0,
            progression = 0,
            cover = DownloadingItem.Cover(
                id = UUID.fromString("6663237f-d940-4a9f-8aa7-30c43b07e7e3"),
                url = URI("https://foo.bar.com/url.jpg")
            ),
            podcast = DownloadingItem.Podcast(
                id = UUID.fromString("fb6cf955-c3da-4bfc-b3a7-275d86d266b5"),
                title = "bar"
            )
        )

        @Test
        fun `with element in downloading queue`() {
            /* Given */
            val downloader = SimpleDownloader()
            whenever(downloaders.of(any())).thenReturn(downloader)
            whenever(repository.addItemToQueue(item1.id)).thenReturn(Mono.empty())
            whenever(repository.findAllToDownload(1)).thenReturn(Flux.just(item1))
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())
            whenever(repository.startItem(item1.id)).thenReturn(Mono.empty())
            idm.addItemToQueue(item1.id).block()

            /* When */
            StepVerifier.create(idm.isInDownloadingQueueById(item1.id))
                /* Then */
                .expectSubscription()
                .expectNext(true)
                .verifyComplete()
        }

        @Test
        fun `with element not in downloading queue`() {
            /* Given */
            /* When */
            StepVerifier.create(idm.isInDownloadingQueueById(UUID.fromString("3372d87e-30a0-4517-ba1f-bfd31e3555cb")))
                /* Then */
                .expectSubscription()
                .expectNext(false)
                .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should move into queue")
    inner class ShouldMoveIntoQueue {

        @Test
        @Timeout(5)
        fun `with success`() {
            /* Given */
            whenever(repository.moveItemInQueue(UUID.fromString("b768eb50-64d2-4707-8da6-6672cc69a4ca"), 3)).thenReturn(Mono.empty())
            whenever(repository.findAllWaiting()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(idm.moveItemInQueue(UUID.fromString("b768eb50-64d2-4707-8da6-6672cc69a4ca"), 3))
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

    }

    @TestConfiguration
    @Import(ItemDownloadManager::class)
    class LocalTestConfiguration {

        @Bean @Qualifier("DownloadExecutor")
        fun downloadExecutor(): ThreadPoolTaskExecutor = Mockito.spy(
            ThreadPoolTaskExecutor().apply {
                corePoolSize = 1
                setThreadNamePrefix("Downloader-")
                initialize()
            }
        )

    }
}

private fun DownloadingItem.toInformation(): DownloadingInformation {
    val fileName = Path(url.path).fileName
    return DownloadingInformation(this, listOf(url), fileName, null)
}

internal class SimpleDownloader: Downloader {

    override lateinit var downloadingInformation: DownloadingInformation
    internal lateinit var itemDownloadManager: ItemDownloadManager

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        this.downloadingInformation = information
        this.itemDownloadManager = itemDownloadManager
        return this
    }

    override fun download(): DownloadingItem = TODO("Not yet implemented")
    override fun startDownload(){}
    override fun stopDownload(){}
    override fun failDownload(){}
    override fun finishDownload(){}
    override fun run(){}
    override fun compatibility(downloadingInformation: DownloadingInformation) = 1
}
