package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
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
@Import(YoutubeDlDownloaderFactory::class)
class YoutubeDlDownloaderFactoryTest(
    @Autowired private val factory: YoutubeDlDownloaderFactory,
) {

    @MockitoBean lateinit var downloaderHelperFactory: DownloaderHelperFactory
    @MockitoBean lateinit var youtubeDL: YoutubeDlService

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
            assertThat(downloader).hasFieldOrPropertyWithValue("youtubeDL", youtubeDL)
            assertThat(downloader).hasFieldOrPropertyWithValue("state", state)
        }
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

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
        fun `should be at lower level of compatibility if multiple urls`() {
            /* Given */
            val dItem = DownloadingInformation(
                item = item,
                urls = listOf("https://foo.bar.com/one.mp3", "https://foo.bar.com/two.mp3").map(URI::create),
                filename = Path("one.mp3"),
                userAgent = null
            )
            /* When */
            val compatibility = factory.compatibility(dItem)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }

        @ParameterizedTest(name = "url {0}")
        @ValueSource(strings = [
            "https://youtube.com/file.mp3",
            "https://www.6play.fr/file.mp3",
            "https://www.tf1.fr/file.mp3",
            "https://www.france.tv/file.mp3",
            "https://replay.gulli.fr/file.mp3",
            "https://dailymotion.com/file.mp3"
        ])
        fun `should be compatible if is video platform from`(url: String) {
            /* Given */
            val dItem = DownloadingInformation(
                item = item,
                urls = listOf(URI.create(url)),
                filename = Path("one.mp3"),
                userAgent = null
            )

            /* When */
            val compatibility = factory.compatibility(dItem)

            /* Then */
            assertThat(compatibility).isEqualTo(5)
        }

        @Test
        fun `should be at lower level minus one for http`() {
            /* Given */
            val dItem = DownloadingInformation(
                item = item,
                urls = listOf(URI.create("https://foo.bar.com/one.mp3")),
                filename = Path("one.mp3"),
                userAgent = null
            )
            /* When */
            val compatibility = factory.compatibility(dItem)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE-1)
        }

        @Test
        fun `should not support other format of urls`() {
            /* Given */
            val dItem = DownloadingInformation(
                item = item,
                urls = listOf(URI.create("rtmp://foo.bar.com/one.mp3")),
                filename = Path("one.mp3"),
                userAgent = null
            )
            /* When */
            val compatibility = factory.compatibility(dItem)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }

    }

}