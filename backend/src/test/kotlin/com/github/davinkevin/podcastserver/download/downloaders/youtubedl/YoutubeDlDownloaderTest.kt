package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.mockito.kotlin.*
import com.sapher.youtubedl.DownloadProgressCallback
import com.sapher.youtubedl.YoutubeDLResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import java.lang.RuntimeException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 08/05/2020
 */
@ExtendWith(SpringExtension::class)
class YoutubeDlDownloaderTest(
        @Autowired private val downloader: YoutubeDlDownloader,
        @Autowired private val idm: ItemDownloadManager,
        @Autowired private val parameters: PodcastServerParameters
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

    @MockBean private lateinit var downloadRepository: DownloadRepository
    @MockBean private lateinit var template: MessagingTemplate
    @MockBean private lateinit var mimeTypeService: MimeTypeService
    @MockBean private lateinit var youtube: YoutubeDlService

    @TestConfiguration
    @Import(YoutubeDlDownloader::class)
    class LocalTestConfiguration {
        @Bean fun clock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
        @Bean fun idm(): ItemDownloadManager = mock()
        @Bean fun parameters() = mock<PodcastServerParameters>().apply {
            whenever(downloadExtension).thenReturn(TEMPORARY_EXTENSION)
        }
    }

    @Nested
    @DisplayName("should download")
    inner class ShouldDownload {

        private val dItem = DownloadingInformation(
                item = item,
                urls = listOf(),
                filename = "one.mp3",
                userAgent = null
        )

        @BeforeEach
        fun beforeEach() {
            Mockito.reset(template, youtube, idm)
            downloader.itemDownloadManager = idm
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
            whenever(downloadRepository.finishDownload(any(), any(), anyOrNull(), any(), any()))
                    .thenReturn(Mono.empty())
        }

        @Test
        fun `with a simple url`(@TempDir rootFolder: Path) {
            /* Given */
            val url = "https://foo.bar.com/one.mp3"
            downloader.downloadingInformation = dItem.copy(urls = listOf(url))

            whenever(parameters.rootfolder).thenReturn(rootFolder)
            whenever(youtube.extractName(url)).thenReturn("one.mp3")
            whenever(youtube.download(eq(url), any(), any())).then {
                val fileToCreate = it.getArgument<Path>(1)
                Files.createFile(fileToCreate)
                mock<YoutubeDLResponse>()
            }

            /* When */
            downloader.download()

            /* Then */
            assertThat(rootFolder.resolve(item.podcast.title).resolve("one-${dItem.item.id}.mp3"))
                    .exists()
        }

        @Nested
        @DisplayName("but fails due to error")
        inner class ButFailsDueToError {

            val url = "https://foo.bar.com/one.mp3"

            @Nested
            @DisplayName("DuringDownload")
            inner class DuringDownload {

                @Test
                fun `with youtube-dl`(@TempDir rootFolder: Path) {
                    /* Given */
                    downloader.downloadingInformation = dItem.copy(urls = listOf(url))

                    whenever(parameters.rootfolder).thenReturn(rootFolder)
                    whenever(youtube.extractName(url)).thenReturn("one.mp3")
                    doThrow(RuntimeException("fake error"))
                            .whenever(youtube).download(eq(url), any(), any())

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
                fun beforeEach(@TempDir rootFolder: Path) {
                    downloader.downloadingInformation = dItem.copy(urls = listOf(url))

                    whenever(parameters.rootfolder).thenReturn(rootFolder)
                    whenever(youtube.extractName(url)).thenReturn("one.mp3")
                }

                @Test
                fun `should throw error if file not created by youtube-dl`() {
                    /* Given */
                    whenever(youtube.download(eq(url), any(), any())).thenReturn(mock())
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
            fun beforeEach(@TempDir rootFolder: Path) {
                Mockito.reset(template, youtube)
                val url = "https://foo.bar.com/one.mp3"
                downloader.downloadingInformation = dItem.copy(urls = listOf(url))
                whenever(parameters.rootfolder).thenReturn(rootFolder)
                whenever(youtube.extractName(url)).thenReturn("one.mp3")
                whenever(youtube.download(eq(url), any(), any())).then {
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
                verify(template, times(2)).sendItem(any())
            }

            @Test
            fun `and should not broadcast`() {
                /* Given */
                val callback = captor.firstValue
                downloader.downloadingInformation = downloader.downloadingInformation.progression(1)

                /* When */
                callback.onProgressUpdate(1f, 2)

                /* Then */
                verify(template, times(1)).sendItem(any())
            }


        }

    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be at lower level of compatibility if multiple urls`() {
            /* Given */
            val dItem = DownloadingInformation(
                    item = item,
                    urls = listOf("https://foo.bar.com/one.mp3", "https://foo.bar.com/two.mp3"),
                    filename = "one.mp3",
                    userAgent = null
            )
            /* When */
            val compatibility = downloader.compatibility(dItem)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }

        @ParameterizedTest(name = "url {0}")
        @ValueSource(strings = [
            "https://youtube.com/file.mp3",
            "https://www.6play.fr/file.mp3",
            "https://www.tf1.fr/file.mp3",
            "https://www.france.tv/file.mp3",
            "https://replay.gulli.fr/file.mp3",
            "https://dailymotion.com/file.mp3"
        ])
        fun `should be compatible if is video platform from`(url: String) {
            /* Given */
            val dItem = DownloadingInformation(
                    item = item,
                    urls = listOf(url),
                    filename = "one.mp3",
                    userAgent = null
            )

            /* When */
            val compatibility = downloader.compatibility(dItem)

            /* Then */
            assertThat(compatibility).isEqualTo(5)
        }

        @Test
        fun `should be at lower level minus one for http`() {
            /* Given */
            val dItem = DownloadingInformation(
                    item = item,
                    urls = listOf("https://foo.bar.com/one.mp3"),
                    filename = "one.mp3",
                    userAgent = null
            )
            /* When */
            val compatibility = downloader.compatibility(dItem)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE-1)
        }

        @Test
        fun `should not support other format of urls`() {
            /* Given */
            val dItem = DownloadingInformation(
                    item = item,
                    urls = listOf("rtmp://foo.bar.com/one.mp3"),
                    filename = "one.mp3",
                    userAgent = null
            )
            /* When */
            val compatibility = downloader.compatibility(dItem)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }

    }

}
