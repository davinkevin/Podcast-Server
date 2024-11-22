package com.github.davinkevin.podcastserver.download.downloaders.ffmpeg

import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status.NOT_DOWNLOADED
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.*
import kotlin.io.path.Path

@ExtendWith(SpringExtension::class)
@Import(FfmpegDownloaderFactory::class)
class FfmpegDownloaderFactoryTest(
    @Autowired private val factory: FfmpegDownloaderFactory
) {

    @MockitoBean lateinit var downloaderHelperFactory: DownloaderHelperFactory
    @MockitoBean lateinit var ffmpegService: FfmpegService
    @MockitoBean lateinit var processService: ProcessService

    private val item: DownloadingItem = DownloadingItem (
        id = UUID.randomUUID(),
        title = "Title",
        status = NOT_DOWNLOADED,
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
    fun `should produce a downloader`() {
        /* Given */
        val state: DownloaderHelper = mock()
        whenever(downloaderHelperFactory.build(any(), any())).thenReturn(state)

        /* When */
        val downloader = factory.with(mock(), mock())

        /* Then */
        assertAll {
            assertThat(downloader).isNotNull
            assertThat(downloader).hasFieldOrPropertyWithValue("state", state)
            assertThat(downloader).hasFieldOrPropertyWithValue("ffmpegService", ffmpegService)
            assertThat(downloader).hasFieldOrPropertyWithValue("processService", processService)
        }
    }


    @Nested
    inner class CompatibilityTest {

        @Test
        fun `should be compatible with multiple urls ending with M3U8 and MP4`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.M3U8", "http://foo.bar.com/end.mp4").map(
                URI::create), Path("end.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @Test
        fun `should be compatible with only urls ending with M3U8`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.m3u8", "http://foo.bar.com/end.M3U8").map(
                URI::create), Path("end.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @Test
        fun `should be compatible with only urls ending with mp4`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.MP4", "http://foo.bar.com/end.mp4").map(
                URI::create), Path("end.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @DisplayName("should be compatible with only one url with extension")
        @ParameterizedTest(name = "{arguments}")
        @ValueSource(strings = ["m3u8", "mp4"])
        fun `should be compatible with only one url with extension`(ext: String) {
            /* Given */
            val di = DownloadingInformation(item, listOf(URI.create("http://foo.bar.com/end.$ext")), Path("end.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(10)
        }

        @DisplayName("should not be compatible with")
        @ParameterizedTest(name = "{arguments}")
        @ValueSource(strings = ["http://foo.bar.com/end.webm", "http://foo.bar.com/end.manifest"])
        fun `should not be compatible with`(url: String) {
            /* Given */
            val di = DownloadingInformation(item, listOf(URI.create(url)), Path("end.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }

}