package com.github.davinkevin.podcastserver.service.health

import com.nhaarman.mockitokotlin2.whenever
import io.vavr.API.Set
import io.vavr.collection.Queue
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.ItemDownloadManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.boot.actuate.health.Status
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
        val first = Item().apply {
                id = UUID.randomUUID()
                title = "first"
        }
        val second = Item().apply {
                id = UUID.randomUUID()
                title = "second"
        }
        val third = Item().apply {
                id = UUID.randomUUID()
                title = "third"
        }
        val fourth = Item().apply {
                id = UUID.randomUUID()
                title = "fourth"
        }

        val downloadingQueue = Set(first)
        val waitingQueue = Queue.of(second, third, fourth)

        whenever(idm.numberOfCurrentDownload).thenReturn(1)
        whenever(idm.limitParallelDownload).thenReturn(3)
        whenever(idm.itemsInDownloadingQueue).thenReturn(downloadingQueue)
        whenever(idm.waitingQueue).thenReturn(waitingQueue)

        /* When */
        val health = downloaderHealthIndicator.health()

        /* Then */
        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details).contains(
                entry("isDownloading", true),
                entry("numberOfParallelDownloads", 3),
                entry("numberOfDownloading", downloadingQueue.length()),
                entry("downloadingItems", downloadingQueue),
                entry("numberInQueue", waitingQueue.length()),
                entry("waitingItems", waitingQueue)
        )
    }

}