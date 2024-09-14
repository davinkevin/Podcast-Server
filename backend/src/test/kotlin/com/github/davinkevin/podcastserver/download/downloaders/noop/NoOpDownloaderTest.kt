package com.github.davinkevin.podcastserver.download.downloaders.noop

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.net.URI
import java.util.*
import kotlin.io.path.Path

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
class NoOpDownloaderTest {

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

    @Test
    fun `should return default value`() {
        /* Given */
        val info = DownloadingInformation(item,  listOf(), Path("noop.mp4"), null)
        val downloader = NoOpDownloaderFactory().with(info, mock())

        /* When */
        downloader.stopDownload()
        downloader.finishDownload()

        /* Then */
        assertThat(downloader.download()).isEqualTo(info.item)
        assertThat(NoOpDownloaderFactory().compatibility(info)).isEqualTo(-1)

        /* And */
        assertThat(downloader.downloadingInformation).isNotNull
            .isSameAs(info)
    }

    @Test
    fun `should return default value with factory`() {
        /* Given */
        val info = DownloadingInformation(item,  listOf(), Path("noop.mp4"), null)
        /* When */
        val factory = NoOpDownloaderFactory()
        /* Then */
        assertThat(factory.compatibility(info)).isEqualTo(-1)
    }

    @Test
    fun `should remove itself from idm when start`() {
        /* GIVEN */
        val idm = mock<ItemDownloadManager>()
        val info = DownloadingInformation(item,  listOf(), Path(""), "")
        val noOpDownloader = NoOpDownloaderFactory().with(info, idm)

        /* WHEN  */
        noOpDownloader.run()

        /* THEN  */
        verify(idm).removeACurrentDownload(any())
    }
}
