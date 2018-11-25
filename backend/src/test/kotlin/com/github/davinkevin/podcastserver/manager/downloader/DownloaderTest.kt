package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.manager.downloader.AbstractDownloader.Companion.WS_TOPIC_DOWNLOAD
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.util.FileSystemUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Created by kevin on 09/02/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class DownloaderTest {

    @Mock lateinit var podcastRepository: PodcastRepository
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var template: SimpMessagingTemplate
    @Mock lateinit var mimeTypeService: MimeTypeService
    @Mock lateinit var itemDownloadManager: ItemDownloadManager
    internal lateinit var downloader: SimpleDownloader

    val item = Item().apply {
        title = "Title"
        url = "http://a.fake.url/with/file.mp4?param=1"
        status = Status.NOT_DOWNLOADED
        numberOfFail = 0
    }
    val podcast = Podcast().apply {
        id = UUID.randomUUID()
        title = "A Fake typeless Podcast"
        items = mutableSetOf()
        add(item)
    }

    @Nested
    inner class SimpleDownloaderTest {
        @BeforeEach
        fun beforeEach() {

            downloader = SimpleDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService)

            whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)
            downloader.postConstruct()

            Try { Files.createDirectories(ROOT_TEST_PATH) }
            FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.title).toFile())
        }

        @Test
        fun `should stop download`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)

            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(podcast.id!!)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            downloader.run()
            downloader.stopDownload()

            /* Then */
            assertThat(item.status).isEqualTo(Status.STOPPED)
            verify(podcastRepository, atLeast(1)).findById(podcast.id!!)
            verify(itemRepository, atLeast(1)).save(item)
            verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item))
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake typeless Podcast").resolve("file.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        fun `should handle finish download without target`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)

            whenever(podcastRepository.findById(eq(podcast.id!!))).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            downloader.finishDownload()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            verify(podcastRepository, atLeast(1)).findById(eq(podcast.id!!))
            verify(itemRepository, atLeast(1)).save(eq(item))
            verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item))
            assertThat(downloader.target).isNull()
        }

        @Test
        fun `should pause a download`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)

            whenever(podcastRepository.findById(eq(podcast.id!!))).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            downloader.pauseDownload()

            /* Then */
            assertThat(item.status).isEqualTo(Status.PAUSED)
            verify(podcastRepository, atLeast(1)).findById(eq(podcast.id!!))
            verify(itemRepository, atLeast(1)).save(eq(item))
            verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item))
        }

        @Test
        fun `should save with same file already existing`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(eq(podcast.id!!))).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            downloader.run()
            downloader.finishDownload()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FINISH)
            assertThat(item.fileName).isEqualTo("file.mp4")
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake typeless Podcast").resolve("file.mp4"))
        }

        @Test
        fun `should failed if error during move`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)
            whenever(podcastRepository.findById(eq(podcast.id!!))).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            downloader.run()
            downloader.target = Paths.get("/tmp", podcast.title, "fake_file$TEMPORARY_EXTENSION")
            assertThatThrownBy { downloader.finishDownload() }
                    .isInstanceOf(RuntimeException::class.java)
                    .hasMessage("Error during move of file")

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
        }

        @Test
        fun `should get the same target file each call`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)

            /* When */
            downloader.target = downloader.getTargetFile(item)
            val target2 = downloader.getTargetFile(item)

            /* Then */
            assertThat(downloader.target).isSameAs(target2)
        }

        @Test
        fun `should handle duplicate on file name`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)

            Files.createDirectory(ROOT_TEST_PATH.resolve(podcast.title))
            Files.createFile(ROOT_TEST_PATH.resolve(podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))

            /* When */
            val targetFile = downloader.getTargetFile(item)

            /* Then */
            assertThat(targetFile).isNotEqualTo(ROOT_TEST_PATH.resolve(podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
        }

        @Test
        fun `should handle error during creation of temp file`() {
            /* Given */
            podcast.title = "bin"
            downloader.with(DownloadingItem(item.setUrl("http://foo.bar.com/bash"),  listOf(), null, null), itemDownloadManager)

            whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get("/"))
            whenever(podcastRepository.findById(eq(podcast.id!!))).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }

            /* When */
            assertThatThrownBy { downloader.getTargetFile(item) }
                    .isInstanceOf(RuntimeException::class.java)
                    .hasMessage("Error during creation of target file")

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
        }

        @Test
        fun `should save sync with podcast`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastRepository.findById(any())).then { throw RuntimeException("Error on find") }

            /* When */
            downloader.saveSyncWithPodcast()

            /* Then */
            assertThat(downloader.item).isSameAs(item)
            verify(itemRepository, never()).save(any())
        }

        @Test
        fun `should throw error if podcast not found`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastRepository.findById(any())).then { Optional.empty<Podcast>() }

            /* When */
            downloader.saveSyncWithPodcast()

            /* Then */
            assertThat(downloader.item).isSameAs(item)
            verify(itemRepository, never()).save(any())
        }


        @Test
        fun `should get item standard`() {
            /* Given */
            downloader.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)

            /* When */
            val itemOfDownloader = downloader.item

            /* Then */
            assertThat(item).isSameAs(itemOfDownloader)
        }
    }

    @Nested
    inner class AlwaysFailingDownloaderTest {

        @Test
        fun `should trigger fail if download fail`() {
            /* Given */
            val d = AlwaysFaillingDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService)
            d.with(DownloadingItem(item,  listOf(), null, null), itemDownloadManager)
            whenever(podcastRepository.findById(podcast.id!!)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.arguments[0] }
            /* When */
            d.run()
            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
        }

    }




    internal class SimpleDownloader(
            itemRepository: ItemRepository,
            podcastRepository: PodcastRepository,
            podcastServerParameters: PodcastServerParameters,
            template: SimpMessagingTemplate,
            mimeTypeService: MimeTypeService
    ) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

        override fun download(): Item {
            try {
                target = getTargetFile(item)
                item.status = Status.FINISH
                Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4$TEMPORARY_EXTENSION"))
                Files.createFile(ROOT_TEST_PATH.resolve(item.podcast.title).resolve("file.mp4"))
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return Item.DEFAULT_ITEM
        }

        override fun compatibility(downloadingItem: DownloadingItem) = 1
    }

    internal class AlwaysFaillingDownloader(
            itemRepository: ItemRepository,
            podcastRepository: PodcastRepository,
            podcastServerParameters: PodcastServerParameters,
            template: SimpMessagingTemplate,
            mimeTypeService: MimeTypeService
    ) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

        override fun download(): Item = throw RuntimeException("I'm failing !")
        override fun compatibility(downloadingItem: DownloadingItem) = 1

    }

    companion object {
        const val TEMPORARY_EXTENSION = ".psdownload"
    }
}
