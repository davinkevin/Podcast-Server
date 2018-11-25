package com.github.davinkevin.podcastserver.manager.downloader

import com.github.davinkevin.podcastserver.entity.Item
import com.nhaarman.mockitokotlin2.mock
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
class NoOpDownloaderTest {

    @Test
    fun `should return default value`() {
        /* Given */
        val noOpDownloader = NoOpDownloader()
        val realItem = Item().apply { url = "foo" }
        val item = DownloadingItem(realItem,  listOf(), "", "")
        /* When */
        noOpDownloader.pauseDownload()
        noOpDownloader.restartDownload()
        noOpDownloader.stopDownload()
        noOpDownloader.finishDownload()

        /* Then */
        assertThat(noOpDownloader.download()).isEqualTo(Item.DEFAULT_ITEM)
        assertThat(noOpDownloader.item).isEqualTo(Item.DEFAULT_ITEM)
        assertThat(noOpDownloader.getItemUrl(realItem)).isEqualTo(realItem.url)
        assertThat(noOpDownloader.compatibility(item)).isEqualTo(-1)
    }

    @Test
    fun `should remove itself from idm when start`() {
        /* GIVEN */
        val idm = mock<ItemDownloadManager>()
        val di = DownloadingItem(Item.DEFAULT_ITEM,  listOf(), "", "")
        val noOpDownloader = NoOpDownloader().apply {
                with(di, idm)
        }

        /* WHEN  */
        noOpDownloader.run()

        /* THEN  */
        verify(idm, times(1)).removeACurrentDownload(Item.DEFAULT_ITEM)
    }
}
