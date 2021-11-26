package com.github.davinkevin.podcastserver.manager.downloader

import com.github.davinkevin.podcastserver.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.FileSystemUtils
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.TimeUnit.SECONDS

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 27/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class RTMPDownloaderTest {

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
    inner class DownloadTest {

        @Mock lateinit var downloadRepository: DownloadRepository
        @Mock lateinit var itemDownloadManager: ItemDownloadManager
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var mimeTypeService: MimeTypeService
        @Mock lateinit var externalTools: ExternalTools
        @Mock lateinit var processService: ProcessService
        val clock: Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
        lateinit var downloader: RTMPDownloader

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

            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)
            whenever(externalTools.rtmpdump).thenReturn("/usr/local/bin/rtmpdump")
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)

            downloader = RTMPDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock, processService, externalTools)

            downloader.with(
                    DownloadingInformation(item,  listOf(), "file.mp4", null),
                    itemDownloadManager
            )

            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(item.podcast.title).toFile())
            Files.createDirectories(ROOT_TEST_PATH)
        }

        @Nested
        @DisplayName("should failed ")
        inner class ShouldFailed {
            //
            @Test
            fun immediatly_when_process_start() {
                /* Given */
                val pb = mock<ProcessBuilder>()
                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(anyVararg())).then { pb }
                whenever(processService.start(any())).then { throw RuntimeException("Error occur during shell execution")}
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(item.podcast.title)))
                        .isEqualTo(0)
            }
            //
            @Test
            fun after_launch_process() {
                /* Given */
                val p = mock<Process>()
                val pb = mock<ProcessBuilder>()
                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(anyVararg())).then { pb }
                whenever(processService.start(any())).then { p }
                whenever(processService.pidOf(p)).then { throw RuntimeException("Error during pid fetching") }
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(item.podcast.title))).isEqualTo(0)
                verify(p).destroy()
            }
        }

        @Nested
        @DisplayName("should download")
        inner class ShouldDownload {

            private val destination = ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file-${item.id}.mp4$TEMPORARY_EXTENSION")
            private val pb = mock<ProcessBuilder>()
            private val p = mock<Process>()
            private val parameters = arrayOf("/usr/local/bin/rtmpdump","-r", item.url.toASCIIString(), "-o", destination.toAbsolutePath().toString())
            private val pid = 1234L

            @BeforeEach
            fun beforeEach() {
                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(*parameters)).then { pb }
                whenever(processService.start(pb)).then {
                    Files.createFile(destination)
                    p
                }
                whenever(processService.pidOf(any())).thenReturn(pid)
                whenever(p.inputStream).then { executionLogs }
            }

            @Test
            fun `and save file to disk`() {
                /* Given */
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
                whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
                whenever(downloadRepository.finishDownload(
                        id = item.id,
                        length = 0,
                        mimeType = "video/mp4",
                        fileName = "file-${item.id}.mp4",
                        downloadDate = fixedDate
                )).thenReturn(Mono.empty())

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FINISH)
                assertThat(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file-${item.id}.mp4")).exists()
            }

            @Nested
            @DisplayName("and then")
            inner class AndThen {

                private var isWaiting = false

                @BeforeEach
                fun beforeEach() {
                    whenever(p.waitFor()).then { isWaiting = true; SECONDS.sleep(4); 0 }
                    whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
                }

                @Nested
                @DisplayName("pause")
                inner class Pause {

                    private val pauseParams = arrayOf("kill", "-STOP", pid.toString())
                    private val pauseProcess = mock<ProcessBuilder>()

                    @BeforeEach
                    fun beforeEach() {
                        doAnswer { pauseProcess }.whenever(processService).newProcessBuilder(*pauseParams)
                    }

                    @Test
                    fun download() {
                        /* GIVEN */
                        whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
                        whenever(downloadRepository.finishDownload(
                                id = item.id,
                                length = 0,
                                mimeType = "video/mp4",
                                fileName = "file-${item.id}.mp4",
                                downloadDate = fixedDate
                        )).thenReturn(Mono.empty())

                        /* WHEN  */
                        runAsync { downloader.run() }
                        await().until { isWaiting }
                        downloader.pauseDownload()

                        /* THEN  */
                        assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.PAUSED)
                        verify(processService).newProcessBuilder("kill", "-STOP", 1234.toString())
                    }

                    @Test
                    fun `but fail`() {
                        /* GIVEN */
                        whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
                        whenever(downloadRepository.finishDownload(
                                id = item.id,
                                length = 0,
                                mimeType = "video/mp4",
                                fileName = "file-${item.id}.mp4",
                                downloadDate = fixedDate
                        )).thenReturn(Mono.empty())
                        doAnswer { throw RuntimeException("Error during -STOP operation on process") }
                                .whenever(processService).start(pauseProcess)

                        /* WHEN  */
                        runAsync { downloader.run() }
                        await().until { isWaiting }
                        downloader.pauseDownload()

                        /* THEN  */
                        assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
                        verify(processService).newProcessBuilder("kill", "-STOP", pid.toString())
                    }

                    @Nested
                    @DisplayName("and restart")
                    inner class AndRestart {

                        private val restartParams = arrayOf("kill", "-SIGCONT", "" + pid.toString())
                        private val restartProcess = mock<ProcessBuilder>()

                        @BeforeEach
                        fun beforeEach() {
                            doAnswer { mock<Process>() }.whenever(processService).start(pauseProcess)
                            doAnswer { restartProcess }.whenever(processService).newProcessBuilder(*restartParams)
                        }

                        @Test
                        fun download() {
                            /* GIVEN */
                            whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
                            whenever(downloadRepository.finishDownload(
                                    id = item.id,
                                    length = 0,
                                    mimeType = "video/mp4",
                                    fileName = "file-${item.id}.mp4",
                                    downloadDate = fixedDate
                            )).thenReturn(Mono.empty())
                            /* WHEN  */
                            runAsync { downloader.run() }
                            await().until { isWaiting }
                            downloader.pauseDownload()
                            downloader.restartDownload()

                            /* THEN  */
                            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STARTED)
                            verify(processService).newProcessBuilder("kill", "-SIGCONT", 1234.toString())
                        }

                        @Test
                        fun `but fail`() {
                            /* GIVEN */
                            whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
                            whenever(downloadRepository.finishDownload(
                                    id = item.id,
                                    length = 0,
                                    mimeType = "video/mp4",
                                    fileName = "file-${item.id}.mp4",
                                    downloadDate = fixedDate
                            )).thenReturn(Mono.empty())
                            doAnswer { throw RuntimeException("Error during -SIGCONT operation on process") }
                                    .whenever(processService).start(restartProcess)

                            /* WHEN  */
                            runAsync { downloader.run() }
                            await().until { isWaiting }
                            downloader.pauseDownload()
                            downloader.restartDownload()

                            /* THEN  */
                            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
                            verify(processService).newProcessBuilder("kill", "-SIGCONT", pid.toString())
                        }
                    }
                }

                @Test
                fun stop() {
                    /* GIVEN */
                    whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
                    whenever(downloadRepository.finishDownload(
                            id = item.id,
                            length = 0,
                            mimeType = "video/mp4",
                            fileName = "file-${item.id}.mp4",
                            downloadDate = fixedDate
                    )).thenReturn(Mono.empty())

                    /* WHEN  */
                    runAsync { downloader.run() }
                    await().until { isWaiting }
                    downloader.stopDownload()

                    /* THEN  */
                    assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STOPPED)
                    verify(p).destroy()
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
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.empty())
                whenever(p.inputStream).then { executionLogs }

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(item.podcast.title)))
                        .isEqualTo(0)
            }
        }
    }

    @Nested
    inner class CompatibilityTest {

        @Mock lateinit var downloadRepository: DownloadRepository
        @Mock lateinit var itemDownloadManager: ItemDownloadManager
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var mimeTypeService: MimeTypeService
        @Mock lateinit var externalTools: ExternalTools
        @Mock lateinit var processService: ProcessService
        val clock: Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
        lateinit var downloader: RTMPDownloader

        @BeforeEach
        fun beforeEach() {
            downloader = RTMPDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock, processService, externalTools)
        }

        @Test
        fun `should be compatible with only one url starting with rtmp`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("rtmp://foo.bar.com/end.M3U8"), "file.mp4", null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible with multiple url`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("rmtp://foo.bar.com/end.m3u8", "rmtp://foo.bar.com/end.M3U8"), "file.mp4", null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible with url not starting by rtmp`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.MP4"), "file.mp4", null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }

    private fun numberOfChildrenFiles(location: Path) = Files
            .newDirectoryStream(location)
            .map { it }
            .size
}
