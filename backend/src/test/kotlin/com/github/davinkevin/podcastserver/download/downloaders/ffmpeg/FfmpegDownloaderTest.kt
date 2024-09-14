package com.github.davinkevin.podcastserver.download.downloaders.ffmpeg


import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status.*
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import com.github.davinkevin.podcastserver.service.storage.FileMetaData
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
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

@ExtendWith(SpringExtension::class)
class FfmpegDownloaderTest(
    @Autowired private val downloader: FfmpegDownloader,
    @Autowired private val helper: DownloaderHelper,
) {

    @MockBean lateinit var ffmpegService: FfmpegService
    @MockBean lateinit var processService: ProcessService

    private val item: DownloadingItem = DownloadingItem (
            id = UUID.randomUUID(),
            title = "Title",
            status = NOT_DOWNLOADED,
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

    @TestConfiguration
    @Import(FfmpegDownloader::class)
    class LocalTestConfiguration {
        @Bean
        fun helper(): DownloaderHelper = DownloaderHelperFactory(
            downloadRepository = mock(),
            clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC")),
            file = mock(),
            template = mock(),
        ).build(mock(), mock())
    }

    @Nested
    inner class DownloaderTest {

        @BeforeEach
        fun beforeEach() {
            helper.info = DownloadingInformation(item, listOf(item.url, URI.create("http://foo.bar.com/end.mp4")), Path("file.mp4"), "Fake UserAgent")
        }

        @Nested
        inner class DownloadOperation {

            @BeforeEach
            fun beforeEach() {
//                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
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
                doNothing().whenever(helper.file).upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == item.podcast.title })
                whenever(helper.file.metadata(eq(item.podcast.title), any()))
                    .thenReturn(FileMetaData("video/mp4", 123L))
                whenever(helper.downloadRepository.finishDownload(
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
        }

        @Nested
        inner class DuringDownloadOperation {

            @Test
            fun should_stop_a_download() {
                /* Given */
                helper.info = helper.info.status(STARTED)
                downloader.process = mock()
//                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)

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
                helper.info = helper.info.status(STARTED)
                downloader.process = mock()
                doAnswer { throw RuntimeException("Error when executing process") }
                        .whenever(downloader.process).destroy()
//                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
                /* When */
                downloader.stopDownload()

                /* Then */
                await().atMost(5, SECONDS).untilAsserted {
                    assertThat(downloader.downloadingInformation.item.status).isEqualTo(FAILED)
                }
            }

        }
    }
}
