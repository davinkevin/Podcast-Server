package com.github.davinkevin.podcastserver.service.health

import com.github.davinkevin.podcastserver.entity.Status.PAUSED
import com.github.davinkevin.podcastserver.entity.Status.STARTED
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.actuate.health.Status
import java.net.URI
import java.util.*

/**
 * Created by kevin on 19/11/2017
 */
@ExtendWith(MockitoExtension::class)
class DownloaderHealthIndicatorTest {

    @Mock lateinit var idm: ItemDownloadManager
    @InjectMocks lateinit var downloaderHealthIndicator: DownloaderHealthIndicator

    @Test
    fun `should generate health information`() {
        /* Given */
        val first = DownloadingItem(
                id = UUID.fromString("6c05149f-a3e1-4302-ab1f-83324c75ad70"),
                url = URI("https://foo.bar.com/1"),
                title = "item1",
                status = com.github.davinkevin.podcastserver.entity.Status.PAUSED,
                progression = 0,
                numberOfFail = 0,
                podcast = DownloadingItem.Podcast(
                        id = UUID.fromString("acaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                        title = "podcast"
                ),
                cover = DownloadingItem.Cover(
                        id = UUID.fromString("cc05149f-a3e1-4302-ab1f-83324c75ad70"),
                        url = URI("https://foo.bar.com/item1/url.png")
                )
        )

        val second = DownloadingItem(
                id = UUID.fromString("7caba8f2-4f2f-49f0-a520-a48bc628d81f"),
                url = URI("https://foo.bar.com/2"),
                title = "item2",
                status = STARTED,
                progression = 10,
                numberOfFail = 0,
                podcast = DownloadingItem.Podcast(
                        id = UUID.fromString("acaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                        title = "podcast"
                ),
                cover = DownloadingItem.Cover(
                        id = UUID.fromString("ccaba8f2-4f2f-49f0-a520-a48bc628d81f"),
                        url = URI("https://foo.bar.com/item2/url.png")
                )
        )

        val third = DownloadingItem(
                id = UUID.fromString("83bc3f2f-d69d-46c8-8e4b-d6db0bce8ad8"),
                url = URI("https://foo.bar.com/3"),
                title = "item3",
                status = PAUSED,
                progression = 50,
                numberOfFail = 0,
                podcast = DownloadingItem.Podcast(
                        id = UUID.fromString("933a15e2-1ec1-4088-b864-4a3414e19306"),
                        title = "podcast2"
                ),
                cover = DownloadingItem.Cover(
                        id = UUID.fromString("630c5bea-0092-490b-bc52-041f9a34a8e2"),
                        url = URI("https://foo.bar.com/item3/url.png")
                )
        )

        val fourth = DownloadingItem(
                id = UUID.fromString("9c1264da-78a7-4e2d-a014-495d3ae39d87"),
                url = URI("https://foo.bar.com/4"),
                title = "item3",
                status = STARTED,
                progression = 70,
                numberOfFail = 0,
                podcast = DownloadingItem.Podcast(
                        id = UUID.fromString("b80138a0-c21d-4bf4-b21f-c445ac61b557"),
                        title = "podcast3"
                ),
                cover = DownloadingItem.Cover(
                        id = UUID.fromString("dc5136dd-21bd-4f32-87c9-4be030720f99"),
                        url = URI("https://foo.bar.com/item4/url.png")
                )
        )

        val downloadingQueue = setOf(first)
        val waitingQueue = ArrayDeque(listOf(second, third, fourth))

        whenever(idm.limitParallelDownload).thenReturn(3)
        whenever(idm.downloadingItems).thenReturn(downloadingQueue)
        whenever(idm.waitingQueue).thenReturn(waitingQueue)

        /* When */
        val health = downloaderHealthIndicator.health()

        /* Then */
        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details).contains(
                entry("isDownloading", true),
                entry("numberOfParallelDownloads", 3),
                entry("numberOfDownloading", downloadingQueue.size),
                entry("downloadingItems", downloadingQueue),
                entry("numberInQueue", waitingQueue.size),
                entry("waitingItems", waitingQueue)
        )
    }

}
