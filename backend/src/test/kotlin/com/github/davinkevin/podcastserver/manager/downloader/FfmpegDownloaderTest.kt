package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.*
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileMetaData
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.io.path.Path

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 20/02/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class FfmpegDownloaderTest {

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

    @Nested
    inner class DownloaderTest {

        @Mock lateinit var downloadRepository: DownloadRepository
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var file: FileStorageService

        @Mock lateinit var itemDownloadManager: ItemDownloadManager

        @Mock lateinit var ffmpegService: FfmpegService
        @Mock lateinit var processService: ProcessService
        @Spy val clock: Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))

        lateinit var downloader: FfmpegDownloader


        @BeforeEach
        fun beforeEach() {
            downloader = FfmpegDownloader(downloadRepository, template, clock, file, ffmpegService, processService)

            downloader
                    .with(DownloadingInformation(item, listOf(item.url, URI.create("http://foo.bar.com/end.mp4")), Path("file.mp4"), "Fake UserAgent"), itemDownloadManager)
        }

        @Nested
        inner class DownloadOperation {

            @BeforeEach
            fun beforeEach() {
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
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
                whenever(processService.waitFor(any())).thenReturn(Result.success(1))
                doAnswer { writeEmptyFileTo(it.getArgument<Path>(0).toString()); null
                }.whenever(ffmpegService).concat(any(), anyVararg())
                doNothing().whenever(file).upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == item.podcast.title })
                whenever(file.metadata(eq(item.podcast.title), any()))
                    .thenReturn(FileMetaData("video/mp4", 123L))
                whenever(downloadRepository.finishDownload(
                        id = item.id,
                        length = 123L,
                        mimeType = "video/mp4",
                        fileName = Path("file-${item.id}.mp4"),
                        downloadDate = fixedDate
                )).thenReturn(0)

                /* When */
                downloader.run()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(downloader.downloadingInformation.item.status).isEqualTo(FINISH)
                    assertThat(downloader.downloadingInformation.item.progression).isEqualTo(100)
                }
            }

            @Test
            fun `should end on FAILED if a download has failed`() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)

                doAnswer { i -> writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }.whenever(ffmpegService).download(eq(item.url.toASCIIString()), any(), any())

                doAnswer { throw RuntimeException("Error during download of other url") }
                    .whenever(ffmpegService).download(eq("http://foo.bar.com/end.mp4"), any(), any())

                whenever(processService.waitFor(any())).thenReturn(Result.success(1))

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isSameAs(FAILED)
            }

            @Test
            @Disabled("to check that, we need to extract the temp file generation")
            fun `should delete all files if error occurred during download`() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)

                doAnswer { i -> writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }
                        .whenever(ffmpegService).download(eq(item.url.toASCIIString()), any(), any())
                doAnswer { throw RuntimeException("Error during download of other url") }
                        .whenever(ffmpegService).download(eq("http://foo.bar.com/end.mp4"), any(), any())
                whenever(processService.waitFor(any())).thenReturn(Result.success(1))

                /* When */
                downloader.run()

                /* Then */
            }

            @Test
            @Disabled("to check that, we need to extract the temp file generation")
            fun `should delete all files if error occurred during concat`() {
                /* Given */
                whenever(ffmpegService.getDurationOf(any(), any())).thenReturn(500.0)

                doAnswer { i -> writeEmptyFileTo(outputPath(i))
                    (0..100).map { it * 5L }.forEach { sendProgress(i, it)}
                    mock<Process>()
                }
                        .whenever(ffmpegService).download(any(), any(), any())

                whenever(processService.waitFor(any())).thenReturn(Result.success(1))
                whenever(ffmpegService.concat(any(), anyVararg())).then {
                    throw RuntimeException("Error during concat operation")
                }

                /* When */
                downloader.run()

                /* Then */
            }

            private fun outputPath(i: InvocationOnMock) = i.getArgument<FFmpegBuilder>(1).build().last()!!
            private fun sendProgress(i: InvocationOnMock, outTimeMs: Long) =
                    i.getArgument<ProgressListener>(2).progress(Progress().apply { out_time_ns = outTimeMs })
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
            fun should_stop_a_download() {
                /* Given */
                downloader.downloadingInformation = downloader.downloadingInformation.status(STARTED)
                downloader.process = mock()
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)

                /* When */
                downloader.stopDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(downloader.downloadingInformation.item.status).isEqualTo(STOPPED)
                    verify(downloader.process).destroy()
                }
            }

            @Test
            fun should_failed_to_stop_a_download() {
                /* Given */
                downloader.downloadingInformation = downloader.downloadingInformation.status(STARTED)
                downloader.process = mock()
                doAnswer { throw RuntimeException("Error when executing process") }
                        .whenever(downloader.process).destroy()
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
                /* When */
                downloader.stopDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(downloader.downloadingInformation.item.status).isEqualTo(FAILED)
                }
            }

        }
    }

    @Nested
    inner class CompatibilityTest {

        @Mock lateinit var downloadRepository: DownloadRepository
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var file: FileStorageService

        @Mock lateinit var itemDownloadManager: ItemDownloadManager

        @Mock lateinit var ffmpegService: FfmpegService
        @Mock lateinit var processService: ProcessService
        @Spy val clock: Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))

        lateinit var downloader: FfmpegDownloader

        @BeforeEach
        fun beforeEach() {
            downloader = FfmpegDownloader(
                downloadRepository,
                template,
                clock,
                file,
                ffmpegService,
                processService
            )
        }

        @Test
        fun `should be compatible with multiple urls ending with M3U8 and MP4`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.M3U8", "http://foo.bar.com/end.mp4").map(URI::create), Path("end.mp4"), null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @Test
        fun `should be compatible with only urls ending with M3U8`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.m3u8", "http://foo.bar.com/end.M3U8").map(URI::create), Path("end.mp4"), null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @Test
        fun `should be compatible with only urls ending with mp4`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.MP4", "http://foo.bar.com/end.mp4").map(URI::create), Path("end.mp4"), null)
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
            val di = DownloadingInformation(item, listOf(URI.create("http://foo.bar.com/end.$ext")), Path("end.mp4"), null)
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
            val di = DownloadingInformation(item, listOf(URI.create(url)), Path("end.mp4"), null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }
}
