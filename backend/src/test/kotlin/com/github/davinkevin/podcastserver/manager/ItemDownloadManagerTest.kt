package com.github.davinkevin.podcastserver.manager

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.manager.selector.ExtractorSelector
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
import reactor.test.StepVerifier
import java.net.URI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Created by kevin on 06/05/15
 */
@ExtendWith(SpringExtension::class)
class ItemDownloadManagerTest(
        @Autowired val downloadExecutor: ThreadPoolTaskExecutor,
        @Autowired val idm: ItemDownloadManager
) {

    @MockBean private lateinit var messaging: MessagingTemplate
    @MockBean private lateinit var downloadRepository: DownloadRepository
    @MockBean private lateinit var parameters: PodcastServerParameters
    @MockBean private lateinit var downloaders: DownloaderSelector
    @MockBean private lateinit var extractors: ExtractorSelector

    private val date = ZonedDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @AfterEach
    fun afterEach() {
        Mockito.reset(messaging)
        idm.apply {
            waitingQueue = ArrayDeque()
            downloadingQueue = mapOf()
        }
        downloadExecutor.corePoolSize = 1
    }

    @Nested
    @DisplayName("should launch download")
    inner class ShouldLaunchDownload {

        @BeforeEach
        fun beforeEach() {
            whenever(parameters.numberOfTry).thenReturn(1)
            whenever(parameters.limitDownloadDate()).thenReturn(date)
        }

        @Test
        fun `with 0 element to download`() {
            /* Given */
            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.empty())

            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                assertThat(idm.waitingQueue).isEmpty()
                verify(messaging).sendWaitingQueue(emptyList())
            }
        }

        val item = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Foo",
                url = URI("https://foo.bar.com/url/com.mp3"),
                status = Status.NOT_DOWNLOADED,
                numberOfFail = 0,
                progression = 0,
                cover = DownloadingItem.Cover(
                        id = UUID.randomUUID(),
                        url = URI("https://foo.bar.com/url.jpg")
                ),
                podcast = DownloadingItem.Podcast(
                        id = UUID.randomUUID(),
                        title = "bar"
                )
        )

        val information = DownloadingInformation(
                item = item,
                filename = "com.mp3",
                urls = listOf("https://foo.bar.com/url/com.mp3"),
                userAgent = null
        )

        @Test
        fun `with 1 element to download`() {
            /* Given */
            val extractor = MockExtractor(information)
            val downloader = mock<Downloader>()

            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.just(item))
            whenever(extractors.of(item.url)).thenReturn(extractor)
            whenever(downloaders.of(information)).thenReturn(downloader)

            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                verify(downloader).run()
                verify(messaging).sendWaitingQueue(emptyList())
            }
        }

        @Test
        fun `with more downloads than possible queue`() {
            /* Given */
            downloadExecutor.corePoolSize = 0
            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.just(item))

            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                verify(messaging).sendWaitingQueue(listOf(item))
            }
        }

        @ParameterizedTest
        @EnumSource(value = Status::class, names = ["STARTED", "FINISH"])
        fun `and doesnt download because item is`(status: Status) {
            /* Given */
            val item = DownloadingItem(
                    id = UUID.randomUUID(),
                    title = "Foo",
                    url = URI("https://foo.bar.com/url/com.mp3"),
                    status = status,
                    numberOfFail = 0,
                    progression = 0,
                    cover = DownloadingItem.Cover(
                            id = UUID.randomUUID(),
                            url = URI("https://foo.bar.com/url.jpg")
                    ),
                    podcast = DownloadingItem.Podcast(
                            id = UUID.randomUUID(),
                            title = "bar"
                    )
            )

            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.just(item))

            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                verify(messaging).sendWaitingQueue(emptyList())
            }
        }

        @Test
        fun `with an error during extraction`() {
            /* Given */
            val downloader = mock<Downloader>()

            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.just(item))
            whenever(extractors.of(item.url)).thenReturn(ErrorExtractor())
            whenever(downloaders.of(information)).thenReturn(downloader)

            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                verify(extractors).of(item.url)
                verify(messaging, times(2)).sendWaitingQueue(emptyList())
            }
        }
    }

    @Nested
    @DisplayName("on queues")
    inner class OnQueues {

        @BeforeEach
        fun beforeEach() {
            whenever(parameters.numberOfTry).thenReturn(1)
            whenever(parameters.limitDownloadDate()).thenReturn(date)
        }

        @AfterEach
        fun afterEach() {
            Mockito.reset(messaging)
            idm.apply {
                waitingQueue = ArrayDeque()
                downloadingQueue = mapOf()
            }
            downloadExecutor.corePoolSize = 1
        }

        private val item1 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Foo",
                url = URI("https://foo.bar.com/url/1/com.mp3"),
                status = Status.NOT_DOWNLOADED,
                numberOfFail = 0,
                progression = 0,
                cover = DownloadingItem.Cover(
                        id = UUID.randomUUID(),
                        url = URI("https://foo.bar.com/url.jpg")
                ),
                podcast = DownloadingItem.Podcast(
                        id = UUID.randomUUID(),
                        title = "bar"
                )
        )

        private val item2 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Foo",
                url = URI("https://foo.bar.com/url/2/com.mp3"),
                status = Status.NOT_DOWNLOADED,
                numberOfFail = 0,
                progression = 0,
                cover = DownloadingItem.Cover(
                        id = UUID.randomUUID(),
                        url = URI("https://foo.bar.com/url.jpg")
                ),
                podcast = DownloadingItem.Podcast(
                        id = UUID.randomUUID(),
                        title = "bar"
                )
        )

        private val item3 = DownloadingItem(
                id = UUID.randomUUID(),
                title = "Foo",
                url = URI("https://foo.bar.com/url/3/com.mp3"),
                status = Status.NOT_DOWNLOADED,
                numberOfFail = 0,
                progression = 0,
                cover = DownloadingItem.Cover(
                        id = UUID.randomUUID(),
                        url = URI("https://foo.bar.com/url.jpg")
                ),
                podcast = DownloadingItem.Podcast(
                        id = UUID.randomUUID(),
                        title = "bar"
                )
        )

        private val information = DownloadingInformation(
                item = item1,
                filename = "com.mp3",
                urls = listOf("https://foo.bar.com/url/1/com.mp3"),
                userAgent = null
        )

        @Test
        fun `should have 2 items in waiting queue`() {
            /* Given */
            val extractor = MockExtractor(information)
            val downloader = mock<Downloader>()

            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.just(item1, item2, item3))
            whenever(extractors.of(item1.url)).thenReturn(extractor)
            whenever(downloaders.of(information)).thenReturn(downloader)

            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                StepVerifier.create(idm.queue)
                        .expectSubscription()
                        .expectNext(item2)
                        .expectNext(item3)
                        .verifyComplete()
            }
        }

        @Test
        fun `should have 1 item in downloading queue`() {
            /* Given */
            val extractor = MockExtractor(information)
            val downloader = mock<Downloader>()

            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.just(item1, item2, item3))
            whenever(extractors.of(item1.url)).thenReturn(extractor)
            whenever(downloaders.of(information)).thenReturn(downloader)
            whenever(downloader.downloadingInformation).thenReturn(information)
            /* When */
            idm.launchDownload()

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                StepVerifier.create(idm.downloading)
                        .expectSubscription()
                        .expectNext(item1)
                        .verifyComplete()
            }

        }
    }

    @Nested
    @DisplayName("should change")
    inner class ShouldChange {
        @Test
        fun `change the number of parallel number`() {
            /* Given */
            whenever(downloadRepository.findAllToDownload(date.toOffsetDateTime(), 1))
                    .thenReturn(Flux.empty())
            /* When */
            idm.setLimitParallelDownload(3)

            /* Then */
            await().atMost(1, SECONDS).untilAsserted {
                verify(messaging).sendWaitingQueue(emptyList())
            }
        }
    }

    @TestConfiguration
    @Import(ItemDownloadManager::class)
    class LocalTestConfiguration {
        @Bean @Qualifier("DownloadExecutor")
        fun downloadExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            setThreadNamePrefix("Downloader-")
            initialize()
        }

    }
}

private fun isAnEmptyQueue() = argWhere<List<DownloadingItem>> { it.isEmpty() }

private class ErrorExtractor: Extractor {
    override fun extract(item: DownloadingItem): DownloadingInformation = throw RuntimeException("Error during extraction")
    override fun compatibility(url: URI): Int = 1
}

private class MockExtractor(private val item: DownloadingInformation): Extractor {
    override fun extract(item: DownloadingItem) = this.item
    override fun compatibility(url: URI) = 1
}
