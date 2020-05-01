package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import com.github.davinkevin.podcastserver.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.FileSystemUtils
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 09/02/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class DownloaderTest {

    private val clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))

    private val item: DownloadingItem = DownloadingItem (
            id = UUID.randomUUID(),
            title = "foo",
            status = Status.NOT_DOWNLOADED,
            url = URI("https://www.bar.com/video/x5ikng3?param=1"),
            numberOfFail = 0,
            progression = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.randomUUID(),
                    title = "baz"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.randomUUID(),
                    url = URI("https://bar/foo/cover.jpg")
            )
    )

    @Nested
    inner class SimpleDownloaderTest {

        var downloadRepository: DownloadRepository = mock()
        var podcastServerParameters: PodcastServerParameters = mock()
        var template: MessagingTemplate = mock()
        var mimeTypeService: MimeTypeService = mock()
        var itemDownloadManager: ItemDownloadManager = mock()
        internal lateinit var downloader: SimpleDownloader

        @BeforeEach
        fun beforeEach() {
            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)

            downloader = SimpleDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock)

            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(item.podcast.title).toFile())
            Try { Files.createDirectories(ROOT_TEST_PATH) }
        }

        @Test
        fun `should stop download`() {
            /* Given */
            downloader
                    .with(DownloadingInformation(item,  listOf(), "filename.mp4", null), itemDownloadManager)

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))

            /* When */
            downloader.run()
            downloader.stopDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STOPPED)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("baz").resolve("filename.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        fun `should handle finish download without target`() {
            /* Given */
            downloader
                    .with(DownloadingInformation(item,  listOf(), "filename.mp4", null), itemDownloadManager)

            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))

            /* When */
            downloader.finishDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target).isNull()
        }

        @Test
        fun `should pause a download`() {
            /* Given */
            downloader
                    .with(DownloadingInformation(item,  listOf(), "filename.mp4", null), itemDownloadManager)

            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))

            /* When */
            downloader.pauseDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.PAUSED)
            verify(template, atLeast(1)).sendItem(any())
        }

        @Test
        fun `should save with same file already existing`() {
            /* Given */
            downloader
                    .with(DownloadingInformation(item,  listOf(), "file.mp4", null), itemDownloadManager)

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))
            whenever(mimeTypeService.probeContentType(any())).thenReturn("video/mp4")
            whenever(downloadRepository.finishDownload(
                    id = item.id,
                    length = 0,
                    mimeType = "video/mp4",
                    fileName = "file.mp4",
                    downloadDate = fixedDate
            )).thenReturn(Mono.empty())

            /* When */
            downloader.run()
            downloader.finishDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FINISH)
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("baz").resolve("file.mp4"))
        }

        @Test
        fun `should failed if error during move`() {
            /* Given */
            downloader
                    .with(DownloadingInformation(item,  listOf(), "file.mp4", null), itemDownloadManager)

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))

            /* When */
            downloader.run()
            downloader.target = Paths.get("/tmp", item.podcast.title, "fake_file$TEMPORARY_EXTENSION")
            assertThatThrownBy { downloader.finishDownload() }
                    .isInstanceOf(RuntimeException::class.java)
                    .hasMessage("Error during move of file")

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.FAILED)
        }

        @Test
        fun `should get the same target file each call`() {
            /* Given */
            val information = DownloadingInformation(item, listOf(), "file.mp4", null)
            downloader
                    .with(information, itemDownloadManager)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)

            /* When */
            downloader.target = downloader.computeTargetFile(information)
            val target2 = downloader.computeTargetFile(information)

            /* Then */
            assertThat(downloader.target).isSameAs(target2)
        }

        @Test
        fun `should handle duplicate on file name`() {
            /* Given */
            val information = DownloadingInformation(item, listOf(), "file.mp4", null)
            downloader.with(information, itemDownloadManager)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)

            Files.createDirectory(ROOT_TEST_PATH.resolve(item.podcast.title))
            Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))

            /* When */
            val targetFile = downloader.computeTargetFile(information)

            /* Then */
            assertThat(targetFile).isNotEqualTo(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        @Disabled
        fun `should handle error during creation of temp file`(@TempDir dir: Path) {
            /* Given */
            val subDir = dir.resolve(UUID.randomUUID().toString())
            val readOnlyFolder = Files.createDirectory(subDir)
            Files.setPosixFilePermissions(readOnlyFolder, setOf(PosixFilePermission.OWNER_READ))

            /* Given */
            // podcast.title = "bin"
            // downloader.with(DownloadingInformation(item.apply { url = "http://foo.bar.com/bash" },  listOf(), null, null), itemDownloadManager)
            val information = DownloadingInformation(item, listOf(), "file.mp4", null)
            downloader.with(information, itemDownloadManager)

            whenever(podcastServerParameters.rootfolder).thenReturn(subDir)

            /* When */
            assertThatThrownBy { downloader.computeTargetFile(information) }
                    .isInstanceOf(RuntimeException::class.java)
                    .hasMessage("Error during creation of target file")

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
        }

        @Test
        @Suppress("UnassignedFluxMonoInstance")
        fun `should save sync with podcast`() {
            /* Given */
            val information = DownloadingInformation(item, listOf(), "file.mp4", null)
            downloader.with(information, itemDownloadManager)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))

            /* When */
            downloader.saveStateOfItem(information.item)

            /* Then */
            verify(downloadRepository, times(1)).updateDownloadItem(information.item)
        }
    }

    @Nested
    inner class AlwaysFailingDownloaderTest {

        var downloadRepository: DownloadRepository = mock()
        var podcastServerParameters: PodcastServerParameters = mock()
        var template: MessagingTemplate = mock()
        var mimeTypeService: MimeTypeService = mock()
        var itemDownloadManager: ItemDownloadManager = mock()
        internal lateinit var downloader: SimpleDownloader

        @Test
        fun `should trigger fail if download fail`() {
            /* Given */
            val d = AlwaysFailingDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock)
            d
                    .with(DownloadingInformation(item,  listOf(), "filename.mp4", null), itemDownloadManager)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(Mono.just(1))

            /* When */
            d.run()

            /* Then */
            assertThat(d.downloadingInformation.item.status).isEqualTo(Status.FAILED)
        }

    }

    companion object {
        const val TEMPORARY_EXTENSION = ".psdownload"
    }
}

internal class SimpleDownloader(
        downloadRepository: DownloadRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        clock: Clock
) : AbstractDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock) {

    override fun download(): DownloadingItem {
        try {
            target = computeTargetFile(downloadingInformation)
            downloadingInformation = downloadingInformation.status(Status.FINISH)
            val item = downloadingInformation.item
            Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4${DownloaderTest.TEMPORARY_EXTENSION}"))
            Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4"))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return downloadingInformation.item
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) = 1
}

internal class AlwaysFailingDownloader(
        downloadRepository: DownloadRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        clock: Clock
) : AbstractDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock) {

    override fun download(): DownloadingItem = throw RuntimeException("I'm failing !")
    override fun compatibility(downloadingInformation: DownloadingInformation) = 1

}
