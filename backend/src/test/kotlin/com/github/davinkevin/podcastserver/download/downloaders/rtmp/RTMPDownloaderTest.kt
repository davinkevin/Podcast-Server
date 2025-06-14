package com.github.davinkevin.podcastserver.download.downloaders.rtmp

import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.storage.FileMetaData
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import org.assertj.core.api.Assertions.assertThat
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
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.io.path.Path

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 27/03/2016 for Podcast Server
 */
@ExtendWith(SpringExtension::class)
class RTMPDownloaderTest(
    @Autowired private val downloader: RTMPDownloader,
    @Autowired private val helper: DownloaderHelper,
) {

    @MockitoBean lateinit var externalTools: ExternalTools
    @MockitoBean lateinit var processService: ProcessService

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

    @TestConfiguration
    @Import(RTMPDownloader::class)
    class LocalTestConfiguration {
        @Bean fun helper(): DownloaderHelper = DownloaderHelperFactory(
            downloadRepository = mock(),
            clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC")),
            file = mock(),
            template = mock(),
        ).build(mock(), mock())
    }

    @Nested
    inner class DownloadTest {

        lateinit var executionLogs: ByteArrayInputStream

        @BeforeEach
        fun beforeEach() {
            executionLogs = """
            Line with no information usable
            Progression : (1%)
            Progression : (2%)
            Progression : (3%)
            Download Complete
        """
                    .trimIndent()
                    .toByteArray()
                    .inputStream()

            whenever(externalTools.rtmpdump).thenReturn("/usr/local/bin/rtmpdump")
            helper.info = DownloadingInformation(item, listOf(), Path("file.mp4"), null)
        }

        @Nested
        @DisplayName("should failed ")
        inner class ShouldFailed {

            @Test
            fun `immediately when process start`() {
                /* Given */
                val pb = mock<ProcessBuilder>()
                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(
                    eq("/usr/local/bin/rtmpdump"),
                    eq("-r"),
                    eq(item.url.toASCIIString()),
                    eq("-o"),
                    any<String>(),
                )).then { pb }
                whenever(processService.start(any())).then { throw RuntimeException("Error occur during shell execution")}
                whenever(helper.downloadRepository.updateDownloadItem(any())).thenReturn(0)

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
            }

            @Test
            fun after_launch_process() {
                /* Given */
                val p = mock<Process>()
                val pb = mock<ProcessBuilder>()
                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(
                    eq("/usr/local/bin/rtmpdump"),
                    eq("-r"),
                    eq(item.url.toASCIIString()),
                    eq("-o"),
                    any<String>(),
                )).then { pb }
                whenever(processService.start(any())).then { p }
                whenever(processService.pidOf(p)).then { throw RuntimeException("Error during pid fetching") }
                whenever(helper.downloadRepository.updateDownloadItem(any())).thenReturn(0)

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
                verify(p).destroy()
            }
        }

        @Nested
        @DisplayName("should download")
        inner class ShouldDownload {

            private val pb = mock<ProcessBuilder>()
            private val p = mock<Process>()
            private val pid = 1234L

            @BeforeEach
            fun beforeEach() {
                var fileToCreate = ""
                Mockito.reset(processService, p, pb)

                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(
                    eq("/usr/local/bin/rtmpdump"),
                    eq("-r"),
                    eq(item.url.toASCIIString()),
                    eq("-o"),
                    any<String>(),
                )).then {
                    fileToCreate = it.arguments[4]!! as String
                    pb
                }
                whenever(processService.start(pb)).then {
                    Files.createFile(Paths.get(fileToCreate))
                    p
                }
                whenever(processService.pidOf(any())).thenReturn(pid)
                whenever(p.inputStream).then { executionLogs }
            }

            @Test
            fun `and save file to disk`() {
                /* Given */
                doNothing().whenever(helper.file)
                    .upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == item.podcast.title })
                whenever(helper.file.metadata(eq(item.podcast.title), any()))
                    .thenReturn(FileMetaData("video/mp4", 123L))
                whenever(helper.downloadRepository.updateDownloadItem(any())).thenReturn(0)
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
                await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                    assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FINISH)
                }
            }

            @Nested
            @DisplayName("and then")
            inner class AndThen {

                private var isWaiting = false

                @BeforeEach
                fun beforeEach() {
                    whenever(p.waitFor()).then { isWaiting = true; SECONDS.sleep(4); 0 }
                    whenever(helper.downloadRepository.updateDownloadItem(any())).thenReturn(0)
                }

                @Test
                fun stop() {
                    /* GIVEN */
                    doNothing().whenever(helper.file)
                        .upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == item.podcast.title })
                    whenever(helper.file.metadata(eq(item.podcast.title), any()))
                        .thenReturn(FileMetaData("video/mp4", 123L))
                    whenever(helper.downloadRepository.finishDownload(
                            id = item.id,
                            length = 123L,
                            mimeType = "video/mp4",
                            fileName = Path("file-${item.id}.mp4"),
                            downloadDate = fixedDate
                    )).thenReturn(0)

                    /* WHEN  */
                    runAsync { downloader.run() }
                    await().until { isWaiting }
                    downloader.stopDownload()

                    /* THEN  */
                    await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                        assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STOPPED)
                        verify(p).destroy()
                    }
                }

            }

            @Test
            fun `and handle unexpected end of download logs`() {
                /* Given */
                val executionLogs = """
                Line with no information usable
                Progression : (1%)
                Progression : (2%)
                Progression : (3%)
                """
                        .trimIndent()
                        .toByteArray()
                        .inputStream()
                whenever(helper.downloadRepository.updateDownloadItem(any())).thenReturn(0)
                whenever(p.inputStream).then { executionLogs }

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
            }
        }
    }

}
