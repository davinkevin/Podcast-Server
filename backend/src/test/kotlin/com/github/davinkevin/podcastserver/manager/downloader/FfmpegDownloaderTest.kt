package com.github.davinkevin.podcastserver.manager.downloader


import arrow.core.Try
import com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.service.*
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.entity.Status.*
import lan.dk.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.manager.downloader.DownloadingItem
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Created by kevin on 20/02/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class FfmpegDownloaderTest {

    @Mock lateinit var podcastRepository: PodcastRepository
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var template: SimpMessagingTemplate
    @Mock lateinit var mimeTypeService: MimeTypeService
    @Mock lateinit var itemDownloadManager: ItemDownloadManager

    @Mock lateinit var urlService: UrlService
    @Mock lateinit var m3U8Service: M3U8Service
    @Mock lateinit var ffmpegService: FfmpegService
    @Mock lateinit var processService: ProcessService

    @InjectMocks lateinit var downloader: FfmpegDownloader

    val aPodcast: Podcast = Podcast().apply {
        title = "M3U8Podcast"
    }
    val item: Item = Item().apply {
        title = "item title"
        podcast = aPodcast
        url = "http://foo.bar/com.m3u8"
        status = STARTED
        numberOfFail = 0
    }

    @Nested
    inner class DownloaderTest {

        @BeforeEach
        fun beforeEach() {
            downloader.setItemDownloadManager(itemDownloadManager)
            downloader.setDownloadingItem(DownloadingItem(item, listOf(item.url, "http://foo.bar.com/end.mp4").toVΛVΓ(), null, "Fake UserAgent"))

            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)
            whenever(podcastRepository.findById(aPodcast.id)).thenReturn(Optional.of(aPodcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(aPodcast.title).toFile())
            Try { Files.createDirectories(ROOT_TEST_PATH) }
            downloader.postConstruct()
        }

        @Nested
        inner class DownloadOperation {

            @BeforeEach
            fun beforeEach() {
                whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            }

            @Test
            fun should_download_file() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)
                whenever(ffmpegService.download(any(), any(), any())).then { i ->
                    writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }
                whenever(processService.waitFor(any())).thenReturn(arrow.core.Try.Success(1))
                doAnswer { writeEmptyFileTo(it.getArgument<Path>(0).toString()); null
                }.whenever(ffmpegService).concat(any(), anyVararg())

                /* When */
                val downloaded = downloader.download()

                /* Then */
                assertThat(ROOT_TEST_PATH.resolve(aPodcast.title).resolve(item.fileName)).exists()
                assertThat(downloaded).isSameAs(item)
                assertThat(downloaded.status).isSameAs(FINISH)
                assertThat(item.progression).isEqualTo(100)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(aPodcast.title)))
                        .isEqualTo(1)
            }

            @Test
            fun `should ends on FAILED if one of download failed`() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)

                doAnswer { i -> writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }
                        .whenever(ffmpegService).download(eq(item.url), any(), any())
                doAnswer { throw RuntimeException("Error during download of other url") }
                        .whenever(ffmpegService).download(eq("http://foo.bar.com/end.mp4"), any(), any())

                whenever(processService.waitFor(any())).thenReturn(arrow.core.Try.Success(1))

                /* When */
                downloader.run()

                /* Then */
                assertThat(item.status).isSameAs(FAILED)
            }

            @Test
            fun `should delete all files if error occurred during download`() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)

                doAnswer { i -> writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }
                        .whenever(ffmpegService).download(eq(item.url), any(), any())
                doAnswer { throw RuntimeException("Error during download of other url") }
                        .whenever(ffmpegService).download(eq("http://foo.bar.com/end.mp4"), any(), any())
                whenever(processService.waitFor(any())).thenReturn(Try.Success(1))

                /* When */
                downloader.run()

                /* Then */
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(aPodcast.title)))
                        .isEqualTo(0)
            }

            @Test
            fun `should delete all files if error occurred during concat`() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)

                doAnswer { i -> writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }
                        .whenever(ffmpegService).download(any(), any(), any())

                whenever(processService.waitFor(any())).thenReturn(arrow.core.Try.Success(1))
                whenever(ffmpegService.concat(any(), anyVararg())).then {
                    throw RuntimeException("Error during concat operation")
                }

                /* When */
                downloader.run()

                /* Then */
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(aPodcast.title)))
                        .isEqualTo(0)
            }

            private fun outputPath(i: InvocationOnMock) = i.getArgument<FFmpegBuilder>(1).build().last()!!
            private fun sendProgress(i: InvocationOnMock, outTimeMs: Long) =
                    i.getArgument<ProgressListener>(2).progress(Progress().apply { out_time_ms = outTimeMs })
            private fun writeEmptyFileTo(location: String): Path =
                    Files.write(Paths.get(location), "".toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            private fun numberOfChildrenFiles(location: Path) = Files
                    .newDirectoryStream(location)
                    .map { it }
                    .size
        }

        @Nested
        inner class DuringDownloadOperation {

            @Test
            fun should_restart_a_current_download() {
                /* Given */
                val restartProcessBuilder = mock<ProcessBuilder>()
                item.status = PAUSED
                downloader.process = mock()
                whenever(processService.newProcessBuilder(anyVararg())).thenReturn(restartProcessBuilder)
                whenever(processService.start(restartProcessBuilder)).thenReturn(mock())

                /* When */
                downloader.restartDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(item.status).isEqualTo(STARTED)
                }
            }

            @Test
            fun should_failed_to_restart() {
                /* Given */
                val restartProcessBuilder = mock<ProcessBuilder>()
                item.status = PAUSED
                downloader.process = mock()
                whenever(processService.newProcessBuilder(anyVararg())).thenReturn(restartProcessBuilder)
                doAnswer { throw RuntimeException("Error when executing process") }
                        .whenever(processService).start(restartProcessBuilder)

                /* When */
                downloader.restartDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(item.status).isEqualTo(FAILED)
                }
            }

            @Test
            fun should_paused_a_download() {
                /* Given */
                val pauseProcess = mock<ProcessBuilder>()
                item.status = STARTED
                downloader.process = mock()
                whenever(processService.newProcessBuilder(anyVararg())).thenReturn(pauseProcess)
                whenever(processService.start(pauseProcess)).thenReturn(mock())

                /* When */
                downloader.pauseDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(item.status).isEqualTo(PAUSED)
                }
            }

            @Test
            fun should_failed_to_pause() {
                /* Given */
                val pauseProcess = mock<ProcessBuilder>()
                item.status = STARTED
                downloader.process = mock()
                whenever(processService.newProcessBuilder(anyVararg())).thenReturn(pauseProcess)
                doAnswer { throw RuntimeException("Error when executing process") }
                        .whenever(processService).start(pauseProcess)

                /* When */
                downloader.pauseDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(item.status).isEqualTo(FAILED)
                }
            }

            @Test
            fun should_stop_a_download() {
                /* Given */
                item.status = STARTED
                downloader.process = mock()

                /* When */
                downloader.stopDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(item.status).isEqualTo(STOPPED)
                    verify(downloader.process).destroy()
                }
            }

            @Test
            fun should_failed_to_stop_a_download() {
                /* Given */
                item.status = STARTED
                downloader.process = mock()
                doAnswer { throw RuntimeException("Error when executing process") }
                        .whenever(downloader.process).destroy()

                /* When */
                downloader.stopDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(item.status).isEqualTo(FAILED)
                }
            }

        }

    }

    @Nested
    inner class CompatibilityTest {

        @Test
        fun `should be compatible with multiple urls ending with M3U8 and MP4`() {
            /* Given */
            val di = DownloadingItem(null, listOf("http://foo.bar.com/end.M3U8", "http://foo.bar.com/end.mp4").toVΛVΓ(), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @Test
        fun `should be compatible with only urls ending with M3U8`() {
            /* Given */
            val di = DownloadingItem(null, listOf("http://foo.bar.com/end.m3u8", "http://foo.bar.com/end.M3U8").toVΛVΓ(), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @Test
        fun `should be compatible with only urls ending with mp4`() {
            /* Given */
            val di = DownloadingItem(null, listOf("http://foo.bar.com/end.MP4", "http://foo.bar.com/end.mp4").toVΛVΓ(), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @DisplayName("should be compatible with only one url with extension")
        @ParameterizedTest(name = "{arguments}")
        @ValueSource(strings = ["m3u8", "mp4"])
        fun `should be compatible with only one url with extension`(ext: String) {
            /* Given */
            val di = DownloadingItem(null, listOf("http://foo.bar.com/end.$ext").toVΛVΓ(), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @DisplayName("should not be compatible with")
        @ParameterizedTest(name = "{arguments}")
        @ValueSource(strings = ["http://foo.bar.com/end.webm", "http://foo.bar.com/end.manifest"])
        fun `should not be compatible with`(url: String) {
            /* Given */
            val di = DownloadingItem(null, listOf(url).toVΛVΓ(), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }
}
