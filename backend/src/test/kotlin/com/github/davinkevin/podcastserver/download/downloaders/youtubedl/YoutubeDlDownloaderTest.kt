package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.service.storage.FileMetaData
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDLResponse
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 08/05/2020
 */
@ExtendWith(SpringExtension::class)
class YoutubeDlDownloaderTest(
    @Autowired private val downloader: YoutubeDlDownloader,
    @Autowired private val helper: DownloaderHelper,
) {

    private val item: DownloadingItem = DownloadingItem (
            id = UUID.randomUUID(),
            title = "Title",
            status = Status.NOT_DOWNLOADED,
            url = URI("http://a.fake.url/with/file.mp4?param=1"),
            numberOfFail = 0,
            progression = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.randomUUID(),
                    title = "A Fake ffmpeg Podcast"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.randomUUID(),
                    url = URI("https://bar/foo/cover.jpg")
            )
    )

    @MockitoBean private lateinit var youtube: YoutubeDlService

    @TestConfiguration
    @Import(YoutubeDlDownloader::class)
    class LocalTestConfiguration {
        @Bean fun idm(): ItemDownloadManager = mock()
        @Bean fun helper(): DownloaderHelper = DownloaderHelperFactory(
            downloadRepository = mock(),
            clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC")),
            file = mock(),
            template = mock(),
        ).build(mock(), mock())
    }

    @Nested
    @DisplayName("should download")
    inner class ShouldDownload {

        private val dItem = DownloadingInformation(
                item = item,
                urls = listOf(),
                filename = Path("one.mp3"),
                userAgent = null
        )

        @BeforeEach
        fun beforeEach() {
            Mockito.reset(helper.template, youtube, helper.manager)

            doNothing().whenever(helper.file).upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == dItem.item.podcast.title })
            whenever(helper.file.metadata(eq(dItem.item.podcast.title), any())).thenReturn(FileMetaData("foo/bar", 123L))
            whenever(helper.downloadRepository.updateDownloadItem(any())).thenReturn(0)
            whenever(helper.downloadRepository.finishDownload(any(), any(), anyOrNull(), any(), any()))
                    .thenReturn(0)
        }

        @Test
        fun `with a simple url`() {
            /* Given */
            val url = URI.create("https://foo.bar.com/one.mp3")
            var finalFile: Path = Files.createTempFile("not-used-for-now", ".mp3")
            helper.info = dItem.copy(urls = listOf(url))

            whenever(youtube.extractName(url.toASCIIString())).thenReturn("one.mp3")
            whenever(youtube.download(eq(url.toASCIIString()), any(), any())).then {
                val fileToCreate = it.getArgument<Path>(1)
                finalFile = Files.createFile(fileToCreate)
                mock<YoutubeDLResponse>()
            }

            /* When */
            downloader.download()

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                verify(helper.file).upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == dItem.item.podcast.title && content == finalFile })
                verify(helper.file).metadata(dItem.item.podcast.title, finalFile)
            }
        }

        @Nested
        @DisplayName("but fails due to error")
        inner class ButFailsDueToError {

            val url: URI = URI.create("https://foo.bar.com/one.mp3")

            @Nested
            @DisplayName("DuringDownload")
            inner class DuringDownload {

                @Test
                fun `with youtube-dl`() {
                    /* Given */
                    helper.info = dItem.copy(urls = listOf(url))

                    whenever(youtube.extractName(url.toASCIIString())).thenReturn("one.mp3")
                    doThrow(RuntimeException("fake error"))
                            .whenever(youtube).download(eq(url.toASCIIString()), any(), any())

                    /* When */
                    assertThatThrownBy { downloader.download() }
                            /* Then */
                            .hasMessage("fake error")
                }

            }

            @Nested
            @DisplayName("DuringFinishOfDownload")
            inner class DuringFinishOfDownload {

                @BeforeEach
                fun beforeEach() {
                    helper.info = dItem.copy(urls = listOf(url))
                    whenever(youtube.extractName(url.toASCIIString())).thenReturn("one.mp3")
                }

                @Test
                fun `should throw error if file not created by youtube-dl`() {
                    /* Given */
                    whenever(youtube.download(eq(url.toASCIIString()), any(), any())).thenReturn(mock())
                    /* When */
                    assertThatThrownBy { downloader.download() }
                            /* Then */
                            .hasMessage("No file found after download with youtube-dl...")
                }

            }

        }

        @Nested
        @DisplayName("and broadcast")
        inner class AndBroadcast {

            private val captor = argumentCaptor<DownloadProgressCallback>()

            @BeforeEach
            fun beforeEach() {
                Mockito.reset(helper.template, youtube)
                val url = URI.create("https://foo.bar.com/one.mp3")
                helper.info = dItem.copy(urls = listOf(url))

                whenever(youtube.extractName(url.toASCIIString())).thenReturn("one.mp3")
                whenever(youtube.download(eq(url.toASCIIString()), any(), any())).then {
                    Files.createFile(it.getArgument(1))
                    mock<YoutubeDLResponse>()
                }
                downloader.download()
                verify(youtube).download(any(), any(), captor.capture())
            }

            @Test
            fun `and broadcast`() {
                /* Given */
                val callback = captor.firstValue
                /* When */
                callback.onProgressUpdate(1f, 2)

                /* Then */
                verify(helper.template, times(2)).sendItem(any())
            }

            @Test
            fun `and should not broadcast`() {
                /* Given */
                val callback = captor.firstValue
                helper.info = helper.info.progression(1)

                /* When */
                callback.onProgressUpdate(1f, 2)

                /* Then */
                await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                    verify(helper.template).sendItem(any())
                }
            }
        }
    }

    @Nested
    @DisplayName("should call state methods")
    inner class ShouldCallStateMethods {

        @Test
        fun `for run`() {
            /* Given */
            val state: DownloaderHelper = mock()
            val aDownloader = YoutubeDlDownloader(state, mock())
            /* When */
            aDownloader.run()
            /* Then */
            verify(state).startDownload(any(), any())
        }

        @Test
        fun `for startDownload`() {
            /* Given */
            val state: DownloaderHelper = mock()
            val aDownloader = YoutubeDlDownloader(state, mock())
            /* When */
            aDownloader.startDownload()
            /* Then */
            verify(state).startDownload(any(), any())
        }

        @Test
        fun `for stopDownload`() {
            /* Given */
            val state: DownloaderHelper = mock()
            val aDownloader = YoutubeDlDownloader(state, mock())
            /* When */
            aDownloader.stopDownload()
            /* Then */
            verify(state).stopDownload()
        }

        @Test
        fun `for failDownload`() {
            /* Given */
            val state: DownloaderHelper = mock()
            val aDownloader = YoutubeDlDownloader(state, mock())
            /* When */
            aDownloader.failDownload()
            /* Then */
            verify(state).failDownload()
        }

    }
}
