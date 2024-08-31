package com.github.davinkevin.podcastserver.download.downloaders.rtmp

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileMetaData
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.service.storage.UploadRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
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
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var file: FileStorageService
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

            whenever(externalTools.rtmpdump).thenReturn("/usr/local/bin/rtmpdump")
            downloader = RTMPDownloader(downloadRepository, template, clock, file, processService, externalTools)

            downloader.with(
                    DownloadingInformation(item,  listOf(), Path("file.mp4"), null),
                    itemDownloadManager
            )
        }

        @Nested
        @DisplayName("should failed ")
        inner class ShouldFailed {

            @Test
            fun immediately_when_process_start() {
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
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)

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
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)

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
                doNothing().whenever(file)
                    .upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == item.podcast.title })
                whenever(file.metadata(eq(item.podcast.title), any()))
                    .thenReturn(FileMetaData("video/mp4", 123L))
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
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
                    whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
                }

                @Test
                fun stop() {
                    /* GIVEN */
                    doNothing().whenever(file)
                        .upload(argThat<UploadRequest.ForItemFromPath> { podcastTitle == item.podcast.title })
                    whenever(file.metadata(eq(item.podcast.title), any()))
                        .thenReturn(FileMetaData("video/mp4", 123L))
                    whenever(downloadRepository.finishDownload(
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
                whenever(downloadRepository.updateDownloadItem(any())).thenReturn(0)
                whenever(p.inputStream).then { executionLogs }

                /* When */
                downloader.run()

                /* Then */
                assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
            }
        }
    }

    @Nested
    inner class CompatibilityTest {

        @Mock lateinit var downloadRepository: DownloadRepository
        @Mock lateinit var itemDownloadManager: ItemDownloadManager
        @Mock lateinit var podcastServerParameters: PodcastServerParameters
        @Mock lateinit var template: MessagingTemplate
        @Mock lateinit var file: FileStorageService
        @Mock lateinit var externalTools: ExternalTools
        @Mock lateinit var processService: ProcessService
        val clock: Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
        lateinit var downloader: RTMPDownloader

        @BeforeEach
        fun beforeEach() {
            downloader = RTMPDownloader(
                downloadRepository,
                template,
                clock,
                file,
                processService,
                externalTools
            )
        }

        @Test
        fun `should be compatible with only one url starting with rtmp`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("rtmp://foo.bar.com/end.M3U8").map(URI::create), Path("file.mp4"), null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible with multiple url`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("rmtp://foo.bar.com/end.m3u8", "rmtp://foo.bar.com/end.M3U8").map(URI::create), Path("file.mp4"), null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible with url not starting by rtmp`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.MP4").map(URI::create), Path("file.mp4"), null)
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
