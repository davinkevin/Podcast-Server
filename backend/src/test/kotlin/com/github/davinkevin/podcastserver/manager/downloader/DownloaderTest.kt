package com.github.davinkevin.podcastserver.manager.downloader

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.writeText

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

/**
 * Created by kevin on 09/02/2016 for Podcast Server
 */
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
        var template: MessagingTemplate = mock()
        var file: FileStorageService = mock()
        var itemDownloadManager: ItemDownloadManager = mock()
        internal lateinit var downloader: SimpleDownloader

        @BeforeEach
        fun beforeEach() {
            downloader = SimpleDownloader(downloadRepository, template, clock, file)
        }

        @Test
        fun `should stop download`() {
            /* Given */
            downloader
                    .with(DownloadingInformation(item,  listOf(), Path("filename.mp4"), null), itemDownloadManager)

            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(1)

            /* When */
            downloader.run()
            downloader.stopDownload()

            /* Then */
            assertThat(downloader.downloadingInformation.item.status).isEqualTo(Status.STOPPED)
            verify(template, atLeast(1)).sendItem(any())
            assertThat(downloader.target.fileName.toString()).isEqualTo("filename-${item.id}.mp4")
        }

        @Test
        @Suppress("UnassignedFluxMonoInstance")
        fun `should save sync with podcast`() {
            /* Given */
            val information = DownloadingInformation(item, listOf(), Path("file.mp4"), null)
            downloader.with(information, itemDownloadManager)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(1)

            /* When */
            downloader.saveStateOfItem(information.item)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                verify(downloadRepository, times(1)).updateDownloadItem(information.item)
            }
        }

        @Test
        @Suppress("UnassignedFluxMonoInstance")
        fun `should fail if error occurs during finish method`() {
            /* Given */
            val information = DownloadingInformation(item, listOf(), Path("file.mp4"), null)
            downloader.with(information, itemDownloadManager)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(1)
            whenever(file.upload(any(), any())).thenThrow(RuntimeException("not expected error"))
            whenever(file.metadata(any(), any())).thenThrow(RuntimeException("not expected error"))

            /* When */
            downloader.apply {
                startDownload()
                assertThat(target).exists()
                finishDownload()
            }

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                assertThat(downloader.target).doesNotExist()
            }
        }
    }

    @Nested
    inner class AlwaysFailingDownloaderTest {

        var downloadRepository: DownloadRepository = mock()
        var template: MessagingTemplate = mock()
        var file: FileStorageService = mock()
        var itemDownloadManager: ItemDownloadManager = mock()
        internal lateinit var downloader: SimpleDownloader

        @Test
        fun `should trigger fail if download fail`() {
            /* Given */
            val d = AlwaysFailingDownloader(downloadRepository, template, clock, file)
            d.with(DownloadingInformation(item,  listOf(), Path("filename.mp4"), null), itemDownloadManager)
            whenever(downloadRepository.updateDownloadItem(any())).thenReturn(1)

            /* When */
            d.run()

            /* Then */
            assertThat(d.downloadingInformation.item.status).isEqualTo(Status.FAILED)
        }
    }
}

internal class SimpleDownloader(
    downloadRepository: DownloadRepository,
    template: MessagingTemplate,
    clock: Clock,
    file: FileStorageService,
) : AbstractDownloader(downloadRepository, template, clock, file) {

    override fun download(): DownloadingItem {
        try {
            target = computeTargetFile(downloadingInformation)
            downloadingInformation = downloadingInformation.status(Status.FINISH)
            Files.createFile(target).apply { writeText("the file is here") }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return downloadingInformation.item
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) = 1
}

internal class AlwaysFailingDownloader(
    downloadRepository: DownloadRepository,
    template: MessagingTemplate,
    clock: Clock,
    file: FileStorageService
) : AbstractDownloader(downloadRepository, template, clock, file) {

    override fun download(): DownloadingItem = throw RuntimeException("I'm failing !")
    override fun compatibility(downloadingInformation: DownloadingInformation) = 1

}
