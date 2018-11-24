package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import com.github.axet.wget.WGet
import com.github.axet.wget.info.DownloadInfo
import com.github.axet.wget.info.URLInfo
import com.github.axet.wget.info.ex.DownloadInterruptedError
import com.github.axet.wget.info.ex.DownloadMultipartError
import com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.factory.WGetFactory
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.util.FileSystemUtils
import java.io.UncheckedIOException
import java.nio.file.Files
import java.util.*

/**
 * Created by kevin on 30/01/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class HTTPDownloaderTest {

    @Nested
    inner class DownloaderTest {

        @Mock lateinit var itemRepository: ItemRepository
        @Mock lateinit var podcastRepository: PodcastRepository
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: SimpMessagingTemplate
        @Mock lateinit var mimeTypeService: MimeTypeService
        @Mock lateinit var wGetFactory: WGetFactory
        @Mock lateinit var urlService: UrlService
        @Mock lateinit var itemDownloadManager: ItemDownloadManager
        @InjectMocks lateinit var downloader: HTTPDownloader

        val item: Item = Item().apply {
            title = "Title"
            url = "http://a.fake.url/with/file.mp4?param=1"
            status = Status.NOT_DOWNLOADED
            numberOfFail = 0
        }
        val podcast: Podcast = Podcast().apply {
            id = UUID.randomUUID()
            title = "A Fake Http Podcast"
            items = mutableSetOf()
            add(item)
        }

        @BeforeEach
        fun beforeEach() {
            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)
            downloader.postConstruct()
            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.title).toFile())

            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.title).toFile())
            Try { Files.createDirectories(ROOT_TEST_PATH) }
        }

        @Test
        fun should_run_download() {
            /* Given */
            val specificUrl = "${item.url}&params=2"
            downloader.with(
                    DownloadingItem(item, listOf(specificUrl), null, null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }
            whenever(urlService.getRealURL(eq(specificUrl), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(specificUrl)).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer {
                Files.createFile(ROOT_TEST_PATH.resolve(podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
                item.status = Status.FINISH
                downloader.finishDownload()
                null
            }.whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FINISH)
            verify(podcastRepository, atLeast(1)).findById(podcast.id)
            verify(itemRepository, atLeast(1)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4"))
        }

        @Test
        fun `should run download by fall-backing to item url`() {
            downloader.with(
                    DownloadingItem(item,  listOf(), null, null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }
            whenever(urlService.getRealURL(eq(item.url), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(item.url)).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer {
                Files.createFile(ROOT_TEST_PATH.resolve(podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
                item.status = Status.FINISH
                downloader.finishDownload()
                null
            }.whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FINISH)
            verify(podcastRepository, atLeast(1)).findById(podcast.id)
            verify(itemRepository, atLeast(1)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4"))
        }

        @Test
        fun `should throw error if no url are found`() {
            val item: Item = Item().apply {
                title = "Title"
            }
            val podcast: Podcast = Podcast().apply {
                id = UUID.randomUUID()
                title = "A Fake Http Podcast"
                add(item)
            }
            downloader.with(
                    DownloadingItem(item,  listOf(), null, null),
                    itemDownloadManager
            )

            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            verify(podcastRepository, atLeast(1)).findById(podcast.id)
            verify(itemRepository, atLeast(1)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
        }

        @Test
        fun should_stop_download() {
            /* Given */
            downloader.with(
                    DownloadingItem(item, listOf(item.url), null, null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { Files.createFile(ROOT_TEST_PATH.resolve(podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION")) }
                    .whenever(wGet).download(any(), any())

            /* When */
            downloader.run()
            downloader.stopDownload()

            /* Then */
            assertThat(item.status).isEqualTo(Status.STOPPED)
            verify(podcastRepository, atLeast(1)).findById(podcast.id)
            verify(itemRepository, atLeast(1)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        fun should_handle_multipart_download_error() {
            /* Given */
            downloader.with(
                    DownloadingItem(item, listOf(item.url), null, null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()
            val error = mock<DownloadMultipartError>()
            val exception = mock<DownloadInfo.Part>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }
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
            assertThat(item.status).isEqualTo(Status.FAILED)
            verify(podcastRepository, atLeast(2)).findById(podcast.id)
            verify(itemRepository, atLeast(2)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
        }

        @Test
        fun should_handle_downloadunterruptedError() {
            /* Given */
            downloader.with(
                    DownloadingItem(item, listOf(item.url), null, null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { throw DownloadInterruptedError() }.whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.STARTED)
            verify(podcastRepository, atLeast(1)).findById(podcast.id)
            verify(itemRepository, atLeast(1)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
        }

        @Test
        fun should_handle_IOException_during_download() {
            /* Given */
            downloader.with(
                    DownloadingItem(item, listOf(item.url), null, null),
                    itemDownloadManager
            )

            val downloadInfo = mock<DownloadInfo>()
            val wGet = mock<WGet>()

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { i -> i.arguments[0] }
            whenever(urlService.getRealURL(any(), any(), any())).then { it.arguments[0] }
            whenever(wGetFactory.newDownloadInfo(any())).thenReturn(downloadInfo)
            whenever(wGetFactory.newWGet(any(), any())).thenReturn(wGet)
            doAnswer { throw mock<UncheckedIOException>() }.whenever(wGet).download(any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            verify(podcastRepository, atLeast(2)).findById(podcast.id)
            verify(itemRepository, atLeast(2)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(AbstractDownloader.WS_TOPIC_DOWNLOAD), same(item))
        }

        @Test
        fun `should be compatible`() {
            /* Given */
            val di = DownloadingItem(item, listOf(item.url), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1)
        }

        @Test
        fun `should not be compatible because multiple url`() {
            /* Given */
            val di = DownloadingItem(item, listOf(item.url, item.url), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible because not starting with http`() {
            /* Given */
            val di = DownloadingItem(item, listOf("ftp://foo.bar.com/file.mp4"), null, null)
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
        val item = Item().apply { url = "http://a.fake.url/with/a/file.mp4"; progression = 0 }

        @BeforeEach
        fun beforeEach() {
            whenever(downloader.item).thenReturn(item)
            whenever(downloader.itemDownloadManager).then { itemDownloadManager }
            whenever(downloader.info).thenReturn(info)

            httpWatcher = HTTPWatcher(downloader)
        }

        @Test
        fun should_do_extraction() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.EXTRACTING)
            whenever(downloader.getItemUrl(item)).thenReturn(item.url)
            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).convertAndSaveBroadcast()
            verify(downloader, never()).stopDownload()
            verify(downloader, never()).finishDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
        }

        @Test
        fun should_do_extraction_done() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.EXTRACTING_DONE)
            whenever(downloader.getItemUrl(item)).thenReturn(item.url)
            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).convertAndSaveBroadcast()
            verify(downloader, never()).stopDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
            verify(downloader, never()).finishDownload()
        }

        @Test
        fun should_do_done() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.DONE)
            whenever(downloader.getItemUrl(item)).thenReturn(item.url)

            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, times(1)).finishDownload()
            verify(itemDownloadManager, times(1)).removeACurrentDownload(any())
            verify(downloader, never()).stopDownload()
            verify(downloader, never()).convertAndSaveBroadcast()
        }

        @Test
        fun should_do_retrying() {
            /* Given */
            whenever(info.state).thenReturn(URLInfo.States.RETRYING)
            whenever(downloader.getItemUrl(item)).thenReturn(item.url)
            /* When */
            httpWatcher.run()

            /* Then */
            verify(downloader, never()).stopDownload()
            verify(downloader, never()).convertAndSaveBroadcast()
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
            verify(downloader, never()).convertAndSaveBroadcast()
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
            verify(downloader, never()).convertAndSaveBroadcast()
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
            assertThat(downloader.item.progression).isEqualTo(50)
            verify(downloader, never()).finishDownload()
            verify(downloader, never()).stopDownload()
            verify(itemDownloadManager, never()).removeACurrentDownload(any())
            verify(downloader, times(1)).convertAndSaveBroadcast()
        }

    }
}
