package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import com.github.axet.vget.VGet
import com.github.axet.vget.info.VGetParser
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import com.github.axet.vget.info.VideoInfo.States.*
import com.github.axet.wget.info.DownloadInfo
import com.github.axet.wget.info.ex.DownloadIOCodeError
import com.github.axet.wget.info.ex.DownloadInterruptedError
import com.github.axet.wget.info.ex.DownloadMultipartError
import com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH
import com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION
import com.github.davinkevin.podcastserver.service.FfmpegService
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.factory.WGetFactory
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.util.FileSystemUtils
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by kevin on 13/02/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class YoutubeDownloaderTest {

    @Mock lateinit var ffmpegService: FfmpegService
    @Mock lateinit var podcastRepository: PodcastRepository
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var itemDownloadManager: ItemDownloadManager
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var template: SimpMessagingTemplate
    @Mock lateinit var mimeTypeService: MimeTypeService
    @Mock lateinit var wGetFactory: WGetFactory
    @InjectMocks lateinit var downloader: YoutubeDownloader

    @Mock lateinit var videoInfo: VideoInfo
    @Mock lateinit var vGetParser: VGetParser

    private val vGet: VGet = mock(defaultAnswer = Answers.RETURNS_SMART_NULLS)
    private val item = Item().apply {
        title = "Title"
        url = "http://a.fake.url/with/file.mp4?param=1"
        status = Status.NOT_DOWNLOADED
        numberOfFail = 0
        progression = 0
    }
    private var podcast = Podcast().apply {
        id = UUID.randomUUID()
        title = "A Fake Youtube Podcast"
        items = mutableSetOf()
        add(item)
    }

    val path = ROOT_TEST_PATH.resolve(podcast.title)!!

    @BeforeEach
    fun beforeEach() {
        whenever(podcastServerParameters.downloadExtension).thenReturn(TEMPORARY_EXTENSION)
        downloader.postConstruct()
        downloader.itemDownloadManager = itemDownloadManager

        Try {
            FileSystemUtils.deleteRecursively(path.toFile())
            Files.createDirectories(ROOT_TEST_PATH)
        }
    }

    @Nested
    @DisplayName("should download ")
    inner class DownloadOperation {

        private var preProgression = item.progression!!
        private val progressionStopChange = {
            if (preProgression == item.progression) true
            else { preProgression = item.progression; false }
        }

        @BeforeEach
        fun beforeEach() {
            whenever(vGet.video).thenReturn(videoInfo)
            whenever(podcastServerParameters.rootfolder).thenReturn(ROOT_TEST_PATH)

            downloader.with(
                    DownloadingItem(item,  listOf(), null, null),
                    itemDownloadManager
            )

            whenever(podcastRepository.findById(podcast.id)).thenReturn(Optional.of(podcast))
            whenever(itemRepository.save(any())).then { it.getArgument(0) }
            whenever(wGetFactory.parser(item.url)).thenReturn(vGetParser)
            whenever(vGetParser.info(URL(item.url))).thenReturn(videoInfo)
            whenever(wGetFactory.newVGet(videoInfo)).thenReturn(vGet)
        }

        @Test
        fun `a low quality video`() {
            /* Given */
            whenever(vGet.getContentExt(any())).thenCallRealMethod()
            val videoList = generate(1)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video low")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it) }.whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FINISH)
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast").resolve("A_super_Name_of_Youtube-Video_low.mp4"))
            assertThat(Files.exists(downloader.target)).isTrue()
            assertThat(Files.exists(downloader.target!!.resolveSibling("A_super_Name_of_Youtube-Video$TEMPORARY_EXTENSION"))).isFalse()
        }

        @Test
        fun `a low quality video with retry during download`() {
            /* Given */
            whenever(vGet.getContentExt(any())).thenCallRealMethod()
            val videoList = generate(1)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video low")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownloadWithRetry(videoInfo, videoList, it) }.whenever(vGet).download(eq(vGetParser), any(), any())
            whenever(ffmpegService.mergeAudioAndVideo(any(), any(), any())).then { Files.createFile(it.getArgument(2)) }

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FINISH)
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast").resolve("A_super_Name_of_Youtube-Video_low.mp4"))
            assertThat(Files.exists(downloader.target)).isTrue()
            assertThat(Files.exists(downloader.target!!.resolveSibling("A_super_Name_of_Youtube-Video$TEMPORARY_EXTENSION"))).isFalse()
        }

        @Test
        fun `a multiple files videos with audio and video files`() {
            /* Given */
            val videoList = generate(2)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video multiple")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it) } .whenever(vGet).download(eq(vGetParser), any(), any())
            whenever(ffmpegService.mergeAudioAndVideo(any(), any(), any())).then { Files.createFile(it.getArgument(2)) }

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FINISH)
            assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast").resolve("A_super_Name_of_Youtube-Video_multiple.mp4"))
            assertThat(Files.exists(downloader.target)).isTrue()
            assertThat(Files.exists(downloader.target!!.resolveSibling("A_super_Name_of_Youtube-Video$TEMPORARY_EXTENSION"))).isFalse()
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(1)
        }

        @Test
        fun `and pause the download`() {
            /* Given */
            val videoList = generate(2)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video multiple")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it, ofMillis(5)) }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            val asyncTask = runAsync { downloader.run() }
            await().atMost(5, SECONDS).until { item.progression > 1 }
            downloader.pauseDownload()

            /* Then */
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
            asyncTask.cancel(true)
        }

        @Test
        fun `and stop the download`() {
            /* Given */
            val videoList = generate(2)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video multiple")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it, ofMillis(5)) }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            val asyncTask = runAsync { downloader.run() }
            await().atMost(5, SECONDS).until { item.progression > 1 }
            downloader.stopDownload()

            /* Then */
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
            asyncTask.cancel(true)
        }

        @Test
        fun `and pause then stop and check if all files are all removed`() {
            /* Given */
            val videoList = generate(2)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video multiple")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it, ofMillis(5)) }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            runAsync { downloader.run() }
            await().atMost(5, SECONDS).until { item.progression > 1 }
            downloader.pauseDownload()
            await().until(progressionStopChange)
            downloader.stopDownload()

            /* Then */
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
        }

        @Test
        fun `and pause then restart the download`() {
            /* Given */
            val videoList = generate(2)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video multiple")
            whenever(ffmpegService.mergeAudioAndVideo(any(), any(), any())).then { Files.createFile(it.getArgument(2)) }
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it, ofMillis(5)) }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            runAsync { downloader.run() }
            await().atMost(5, SECONDS).until { item.progression > 1 }
            downloader.pauseDownload()
            await().until(progressionStopChange)
            downloader.restartDownload()

            /* Then */
            await().atMost(5, SECONDS).untilAsserted {
                assertThat(item.status).isEqualTo(Status.FINISH)
                assertThat(downloader.target).isEqualTo(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast").resolve("A_super_Name_of_Youtube-Video_multiple.mp4"))
                assertThat(Files.exists(downloader.target)).isTrue()
                assertThat(Files.exists(downloader.target!!.resolveSibling("A_super_Name_of_Youtube-Video$TEMPORARY_EXTENSION"))).isFalse()
                assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(1)
            }
        }

        @Test
        fun `and failed due to title not available`() {
            /* Given */

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            assertThat(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast")).doesNotExist()
        }

        @Test
        fun `and failed if destination can't be written`() {
            /* Given */
            whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get("/bin/foo/"))
            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video stop")
            whenever(videoInfo.info).thenReturn(generate(3))

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            assertThat(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast")).doesNotExist()
        }

        @Test
        fun `and failed due to error of download`() {
            /* Given */
            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video interruption")
            doAnswer { throw DownloadInterruptedError() }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
        }

        @Test
        fun `and failed due to error during download`() {
            /* Given */
            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video interruption")
            doAnswer { simulateDownloadInError(videoInfo, it) }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
        }

        @Test
        fun `and failed due to multipart error`() {
            /* Given */
            val info = mock<DownloadInfo>()
            val part1 = mock<DownloadInfo.Part>()
            val part2 = mock<DownloadInfo.Part>()
            whenever(part1.exception).then { RuntimeException("This is a downloadPart Error") }
            whenever(info.parts).then { listOf(part1, part2) }

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video multipart error")
            doAnswer { throw DownloadMultipartError(info) }
                    .whenever(vGet).download(eq(vGetParser), any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
            verify(info).parts
            verify(part1).exception
        }

        @Test
        fun `and failed with error during the finish operation`() {
            /* Given */
            whenever(vGet.getContentExt(any())).thenCallRealMethod()
            val videoList = generate(2)

            whenever(videoInfo.title).thenReturn("A super Name of Youtube-Video low")
            whenever(videoInfo.info).thenReturn(videoList)
            doAnswer{ simulateDownload(videoInfo, videoList, it) }.whenever(vGet).download(eq(vGetParser), any(), any())
            doAnswer { throw RuntimeException("Error during merge") }
                    .whenever(ffmpegService).mergeAudioAndVideo(any(), any(), any())

            /* When */
            downloader.run()

            /* Then */
            assertThat(item.status).isEqualTo(Status.FAILED)
            assertThat(numberOfChildrenFiles(ROOT_TEST_PATH.resolve("A Fake Youtube Podcast"))).isEqualTo(0)
        }
    }

    @Nested
    inner class Compatibility {

        val item = Item().apply {
            url = "https://www.youtube.com/a/super/video"
        }

        @Test
        fun `should be compatible`() {
            /* Given */
            val di = DownloadingItem(item, listOf(item.url), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible because multiple url`() {
            /* Given */
            val di = DownloadingItem(item, listOf(item.url, item.url), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible because not starting with http`() {
            /* Given */
            val di = DownloadingItem(item, listOf("ftp://foo.bar.com/file.mp4"), null, null)
            /* When */
            val compatibility = downloader.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }

    @Nested
    inner class Watcher {

        lateinit var downloader: YoutubeDownloader

        @BeforeEach
        fun beforeEach() {
            downloader = mock()
        }

        @Test
        fun `should failed if retry took more than defined time`() {
            /* Given */
            val watcher = YoutubeDownloader.YoutubeWatcher(downloader, Duration.ofNanos(1))
            whenever(downloader.v).then { vGet }
            whenever(vGet.video).thenReturn(videoInfo)
            whenever(videoInfo.info).then { listOf<VideoFileInfo>() }
            whenever(downloader.item).then { Item.DEFAULT_ITEM }
            whenever(videoInfo.state).then { RETRYING }
            whenever(videoInfo.exception).then { DownloadIOCodeError(512) }
            whenever(videoInfo.delay).then { 0 }

            /* When */
            watcher.run()

            /* Then */
            verify(downloader).failDownload()
        }

    }

    private fun generate(number: Int): List<VideoFileInfo> {
        return (0 until number)
                .map { VideoFileInfo(null).apply {
                    targetFile = path.resolve("tmp_$it.tmp").toFile()
                    contentType = if (it == 0) "video/mp4" else "audio/webm"
                    length = it * 1000L
                } }

    }

    private fun simulateDownload(videoInfo: VideoInfo,
                                 videoList: List<VideoFileInfo>,
                                 i: InvocationOnMock,
                                 awaitDuration: Duration = ZERO
    ) {
        val runnable = i.getArgument<Runnable>(2)
        val atomicBoolean = i.getArgument<AtomicBoolean>(1)

        doAnswer { EXTRACTING_DONE }.whenever(videoInfo).state
        runnable.run()

        (0..100).forEach { cpt ->
            doAnswer { if (atomicBoolean.get()) STOP else DOWNLOADING }
                    .whenever(videoInfo).state

            videoList.forEach { it.count = it.length / 100 * cpt }
            runnable.run()
            if(atomicBoolean.get())
                throw DownloadInterruptedError()

            MILLISECONDS.sleep(awaitDuration.toMillis())
        }
        videoList.forEach { Try { Files.createFile(it.target.toPath()) } }

        doAnswer { DONE }.whenever(videoInfo).state
        runnable.run()
    }

    private fun simulateDownloadInError(videoInfo: VideoInfo, i: InvocationOnMock) {
        val runnable = i.getArgument<Runnable>(2)
        doAnswer { ERROR }.whenever(videoInfo).state
        runnable.run()
    }

    private fun simulateDownloadWithRetry(info: VideoInfo, videoList: List<VideoFileInfo>, i: InvocationOnMock) {
        val runnable = i.getArgument<Runnable>(2)

        whenever(info.exception).then { DownloadIOCodeError(512) }
        whenever(info.delay).then { 0 }
        doAnswer { RETRYING }.whenever(info).state
        runnable.run()

        whenever(info.exception).then { DownloadIOCodeError(512) }
        whenever(info.delay).then { 1 }
        doAnswer { RETRYING }.whenever(info).state
        runnable.run()

        simulateDownload(info, videoList, i)
    }
}

private fun numberOfChildrenFiles(location: Path) = Files
        .newDirectoryStream(location)
        .map { it }
        .size