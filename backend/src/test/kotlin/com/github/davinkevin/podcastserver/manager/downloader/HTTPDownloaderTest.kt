package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import com.github.axet.wget.WGet
import com.github.axet.wget.info.DownloadInfo
import com.github.axet.wget.info.URLInfo
import com.github.axet.wget.info.ex.DownloadInterruptedError
import com.github.axet.wget.info.ex.DownloadMultipartError
import com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.service.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.factory.WGetFactory
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.FileSystemUtils
import reactor.core.publisher.Mono
import java.io.UncheckedIOException
import java.net.URI
import java.nio.file.Files
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 30/01/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class HTTPDownloaderTest {

    private val item: DownloadingItem = DownloadingItem (
            id = UUID.randomUUID(),
            title = "Title",
            status = Status.NOT_DOWNLOADED,
            url = URI("http://a.fake.url/with/file.mp4?param=1"),
            numberOfFail = 0,
            progression = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.randomUUID(),
                    title = "A Fake Http Podcast"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.randomUUID(),
                    url = URI("https://bar/foo/cover.jpg")
            )
    )

    @Nested
    inner class DownloaderTest {

        @Mock lateinit var downloadRepository: DownloadRepository
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var mimeTypeService: MimeTypeService
        @Mock lateinit var wGetFactory: WGetFactory
        @Mock lateinit var urlService: UrlService
        @Mock lateinit var itemDownloadManager: ItemDownloadManager
        @Spy val clock: Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
        lateinit var downloader: HTTPDownloader

        @BeforeEach
        fun beforeEach() {
            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)

            downloader = HTTPDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock, urlService, wGetFactory)

            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(item.podcast.title).toFile())
            Try { Files.createDirectories(ROOT_TEST_PATH) }
        }

        @Test
        fun `should run download with specific url in information`() {
            /* Given */
            val specificUrl = "${item.url}&params=2"
            downloader.with(
                    DownloadingInformation(item, listOf(specificUrl), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
            whenever(urlService.getRealURL(eq(specificUrl), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(specificUrl)).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer {
                Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
                downloader.finishDownload()
                null
            }.whenever(wGet).download(any(), any())
            whenever(downloadRepository.finishDownload(
                    id = item.id,
                    length = 0,
                    mimeType = "video/mp4",
                    fileName = "file.mp4",
                    downloadDate = fixedDate
            )).thenReturn(Mono.empty())


            /* When */
            downloader.run()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FINISH)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4"))
        }

        @Test
        fun `should run download by fall-backing to item url`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
            whenever(urlService.getRealURL(eq(item.url.toASCIIString()), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(item.url.toASCIIString())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer {
                Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
                downloader.finishDownload()
                null
            }.whenever(wGet).download(any(), any())
            whenever(downloadRepository.finishDownload(
                    id = item.id,
                    length = 0,
                    mimeType = "video/mp4",
                    fileName = "file.mp4",
                    downloadDate = fixedDate
            )).thenReturn(Mono.empty())


            /* When */
            downloader.run()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FINISH)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4"))
        }

        @Test
        fun `should stop download`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION")) }
                    .whenever(wGet).download(any(), any())

            /* When */
            downloader.run()
            downloader.stopDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STOPPED)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        fun `should pause download`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION")) }
                    .whenever(wGet).download(any(), any())

            /* When */
            downloader.run()
            downloader.pauseDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.PAUSED)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        fun `should handle multipart download`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()
            val error = mock<DownloadMultipartError>()
            val exception = mock<DownloadInfo.Part>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            whenever(error.info).thenReturn(downloadInfo)
            whenever(exception.exception).then { Exception() }
            whenever(downloadInfo.parts).thenReturn(listOf(mock(), mock(), mock(), exception))
            doThrow(error).whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
            verify(template, atLeast(1)).sendItem(any())
        }

        @Test
        fun `should handle downloadunterruptedError`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { throw DownloadInterruptedError() }.whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STARTED)
            verify(template, atLeast(1)).sendItem(any())
        }

        @Test
        fun `should handle IO Exception during download`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { throw mock<UncheckedIOException>() }.whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
            verify(template, atLeast(1)).sendItem(any())
        }

        @Test
        fun `should update progression of download`() {
            /* Given */
            downloader.with(
                    DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION")) }
                    .whenever(wGet).download(any(), any())

            /* When */
            downloader.apply {
                run()
                progression(50)
            }

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STARTED)
            assertThat(downloader.downloadingInformation.item.progression).isEqualTo(50)
        }

        @Test
        fun `should be compatible`() {
            /* Given */
            val di = DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1)
        }

        @Test
        fun `should not be compatible because multiple url`() {
            /* Given */
            val di = DownloadingInformation(item, listOf(item.url.toASCIIString(), item.url.toASCIIString()), "file.mp4", null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible because not starting with http`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("ftp://foo.bar.com/file.mp4"), "file.mp4", null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }

    @Nested
    inner class WatcherTest {

        @Mock lateinit var info: DownloadInfo
        @Mock lateinit var itemDownloadManager: ItemDownloadManager
        @Mock lateinit var downloader: HTTPDownloader
        lateinit var httpWatcher: HTTPWatcher


        @BeforeEach
        fun beforeEach() {
            val information = DownloadingInformation(item, listOf(item.url.toASCIIString()), "file.mp4", null)
            whenever(downloader.downloadingInformation).thenReturn(information)
            whenever(downloader.itemDownloadManager).thenReturn(itemDownloadManager)
            whenever(downloader.info).thenReturn(info)

            httpWatcher = HTTPWatcher(downloader)
        }

        @Test
        fun `should do extraction`() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.EXTRACTING)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).broadcast(any())
            verify(downloader, never()).stopDownload()
            verify(downloader, never()).finishDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
        }

        @Test
        fun `should done extraction`() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.EXTRACTING_DONE)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).broadcast(any())
            verify(downloader, never()).stopDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
            verify(downloader, never()).finishDownload()
        }

        @Test
        fun `should do done`() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.DONE)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, times(1)).finishDownload()
            verify(itemDownloadManager, times(1)).removeACurrentDownload(any())
            verify(downloader, never()).stopDownload()
            verify(downloader, never()).broadcast(any())
        }

        @Test
        fun should_do_retrying() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.RETRYING)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).stopDownload()
            verify(downloader, never()).broadcast(any())
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
            verify(downloader, never()).finishDownload()
        }

        @Test
        fun should_do_stop() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.STOP)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).finishDownload()
            verify(downloader, never()).broadcast(any())
            verify(downloader, never()).stopDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
        }

        @Test
        fun should_do_downloading_without_length() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.DOWNLOADING)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).finishDownload()
            verify(downloader, never()).broadcast(any())
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
            verify(downloader, never()).stopDownload()
        }

        @Test
        fun should_do_downloading_with_length() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.DOWNLOADING)
            whenever(info.count).thenReturn(5L)
            whenever(info.length).thenReturn(10L)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, times(1)).progression(50)
            verify(downloader, never()).finishDownload()
            verify(downloader, never()).stopDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
        }

    }
}
