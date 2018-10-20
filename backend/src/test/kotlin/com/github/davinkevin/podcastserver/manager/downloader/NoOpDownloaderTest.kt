package com.github.davinkevin.podcastserver.manager.downloader

import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.mock
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.manager.downloader.DownloadingItem
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
        val item = DownloadingItem(Item.DEFAULT_ITEM, listOf<String>().toVΛVΓ(), "", "")

        /* When */
        noOpDownloader.pauseDownload()
        noOpDownloader.restartDownload()
        noOpDownloader.stopDownload()
        noOpDownloader.finishDownload()

        /* Then */
        assertThat(noOpDownloader.download()).isEqualTo(Item.DEFAULT_ITEM)
        assertThat(noOpDownloader.item).isEqualTo(Item.DEFAULT_ITEM)
        assertThat(noOpDownloader.getItemUrl(Item.DEFAULT_ITEM)).isNull()
        assertThat(noOpDownloader.compatibility(item)).isEqualTo(-1)
    }

    @Test
    fun `should remove itself from idm when start`() {
        /* GIVEN */
        val idm = mock<ItemDownloadManager>()
        val di = DownloadingItem(Item.DEFAULT_ITEM, listOf<String>().toVΛVΓ(), "", "")
        val noOpDownloader = NoOpDownloader().apply {
                setDownloadingItem(di)
                setItemDownloadManager(idm)
        }

        /* WHEN  */
        noOpDownloader.run()

        /* THEN  */
        verify(idm, times(1)).removeACurrentDownload(Item.DEFAULT_ITEM)
    }
}
