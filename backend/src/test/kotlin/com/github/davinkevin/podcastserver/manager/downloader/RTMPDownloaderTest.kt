package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.STARTED
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.util.FileSystemUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Created by kevin on 27/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class RTMPDownloaderTest {

    @Mock lateinit var externalTools: ExternalTools
    @Mock lateinit var podcastRepository: PodcastRepository
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var itemDownloadManager: ItemDownloadManager
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var template: SimpMessagingTemplate
    @Mock lateinit var mimeTypeService: MimeTypeService
    @Mock lateinit var processService: ProcessService
    @InjectMocks lateinit var downloader: RTMPDownloader

    private val aPodcast = Podcast().apply {
        id = UUID.randomUUID()
        title = "RTMP Podcast"
    }
    private val item = Item().apply {
        url = "rtmp://a.url.com/foo/bar.mp4"
        status = STARTED
        podcast = aPodcast
        progression = 0
        numberOfFail = 0
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

            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)
            whenever(externalTools.rtmpdump).thenReturn("/usr/local/bin/rtmpdump")
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(ArgumentMatchers.eq(aPodcast.id))).thenReturn(Optional.of(aPodcast))

            downloader.with(
                    DownloadingItem(item,  listOf(), null, null),
                    itemDownloadManager
            )
            downloader.postConstruct()

            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(aPodcast.title).toFile())
            Try { Files.createDirectories(ROOT_TEST_PATH) }
        }

        @Nested
        @DisplayName("should failed ")
        inner class ShouldFailed {

            @Test
            fun immediatly_when_process_start() {
                /* Given */
                val pb = mock<ProcessBuilder>()
                whenever(pb.directory(File("/tmp"))).then { pb }
                whenever(pb.redirectErrorStream(true)).then { pb }
                whenever(processService.newProcessBuilder(anyVararg())).then { pb }
                whenever(processService.start(any())).then { throw RuntimeException("Error occur during shell execution")}

                /* When */
                downloader.run()

                /* Then */
                assertThat(item.status).isEqualTo(Status.FAILED)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(item.podcast.title)))
                        .isEqualTo(0)
            }

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

                /* When */
                downloader.run()

                /* Then */
                assertThat(item.status).isEqualTo(Status.FAILED)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(item.podcast.title)))
                        .isEqualTo(0)
                verify(p).destroy()
            }
        }

        @Nested
        @DisplayName("should download")
        inner class ShouldDownload {

            private val destination = ROOT_TEST_PATH.resolve(item.podcast.title).resolve("bar.mp4$TEMPORARY_EXTENSION")
            private val pb = mock<ProcessBuilder>()
            private val p = mock<Process>()
            private val parameters = arrayOf("/usr/local/bin/rtmpdump","-r", item.url, "-o", destination.toAbsolutePath().toString())
            private val pid = 1234

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
                /* When */
                downloader.run()

                /* Then */
                assertThat(item.status).isEqualTo(Status.FINISH)
                assertThat(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("bar.mp4")).exists()
            }

            @Nested
            @DisplayName("and then")
            inner class AndThen {

                private var isWaiting = false

                @BeforeEach
                fun beforeEach() {
                    whenever(p.waitFor()).then { isWaiting = true; SECONDS.sleep(4) }
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
                        /* WHEN  */
                        runAsync { downloader.run() }
                        await.until { isWaiting }
                        downloader.pauseDownload()

                        /* THEN  */
                        assertThat(item.status).isEqualTo(Status.PAUSED)
                        verify(processService).newProcessBuilder("kill", "-STOP", 1234.toString())
                    }

                    @Test
                    fun `but fail`() {
                        /* GIVEN */
                        doAnswer { throw RuntimeException("Error during -STOP operation on process") }
                                .whenever(processService).start(pauseProcess)

                        /* WHEN  */
                        runAsync { downloader.run() }
                        await.until { isWaiting }
                        downloader.pauseDownload()

                        /* THEN  */
                        assertThat(item.status).isEqualTo(Status.FAILED)
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
                            /* WHEN  */
                            runAsync { downloader.run() }
                            await.until { isWaiting }
                            downloader.pauseDownload()
                            downloader.restartDownload()

                            /* THEN  */
                            assertThat(item.status).isEqualTo(Status.STARTED)
                            verify(processService).newProcessBuilder("kill", "-SIGCONT", 1234.toString())
                        }

                        @Test
                        fun `but fail`() {
                            /* GIVEN */
                            doAnswer { throw RuntimeException("Error during -SIGCONT operation on process") }
                                    .whenever(processService).start(restartProcess)

                            /* WHEN  */
                            runAsync { downloader.run() }
                            await.until { isWaiting }
                            downloader.pauseDownload()
                            downloader.restartDownload()

                            /* THEN  */
                            assertThat(item.status).isEqualTo(Status.FAILED)
                            verify(processService).newProcessBuilder("kill", "-SIGCONT", pid.toString())
                        }
                    }

                }

                @Test
                fun stop() {
                    /* GIVEN */
                    /* WHEN  */
                    runAsync { downloader.run() }
                    await.until { isWaiting }
                    downloader.stopDownload()

                    /* THEN  */
                    assertThat(item.status).isEqualTo(Status.STOPPED)
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

                whenever(p.inputStream).then { executionLogs }

                /* When */
                downloader.run()

                /* Then */
                assertThat(item.status).isEqualTo(Status.FAILED)
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve(item.podcast.title)))
                        .isEqualTo(0)
            }
        }
    }

    @Nested
    inner class CompatibilityTest {

        @Test
        fun `should be compatible with only one url starting with rtmp`() {
            /* Given */
            val di = DownloadingItem(item, listOf("rtmp://foo.bar.com/end.M3U8"), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible with multiple url`() {
            /* Given */
            val di = DownloadingItem(item, listOf("rmtp://foo.bar.com/end.m3u8", "rmtp://foo.bar.com/end.M3U8"), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible with url not starting by rtmp`() {
            /* Given */
            val di = DownloadingItem(item, listOf("http://foo.bar.com/end.MP4"), null, null)
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
