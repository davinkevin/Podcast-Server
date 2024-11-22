package com.github.davinkevin.podcastserver.download.downloaders.rtmp

import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
@Import(RTMPDownloaderFactory::class)
class RTMPDownloaderFactoryTest(
    @Autowired private val factory: RTMPDownloaderFactory
) {
    @MockitoBean lateinit var downloaderHelperFactory: DownloaderHelperFactory
    @MockitoBean lateinit var processService: ProcessService
    @MockitoBean lateinit var externalTools: ExternalTools

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
            assertThat(downloader).hasFieldOrPropertyWithValue("externalTools", externalTools)
            assertThat(downloader).hasFieldOrPropertyWithValue("processService", processService)
            assertThat(downloader).hasFieldOrPropertyWithValue("state", state)
        }
    }

    @Nested
    inner class CompatibilityTest {

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
        fun `should be compatible with only one url starting with rtmp`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("rtmp://foo.bar.com/end.M3U8").map(URI::create), Path("file.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible with multiple url`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("rmtp://foo.bar.com/end.m3u8", "rmtp://foo.bar.com/end.M3U8").map(
                URI::create), Path("file.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible with url not starting by rtmp`() {
            /* Given */
            val di = DownloadingInformation(item, listOf("http://foo.bar.com/end.MP4").map(URI::create), Path("file.mp4"), null)
            /* When */
            val compatibility = factory.compatibility(di)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }


}