package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.download.downloaders.youtubedl.YoutubeDlDownloader
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.downloader.FfmpegDownloader
import com.github.davinkevin.podcastserver.manager.downloader.RTMPDownloader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.*
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.reflect.KClass

@ExtendWith(SpringExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DownloaderSelectorTest(
    @Autowired val applicationContext: ApplicationContext
) {

    @MockBean private lateinit var ffmpegDownloader: FfmpegDownloader
    @MockBean private lateinit var rtmpDownloader: RTMPDownloader
    @MockBean private lateinit var youtubeDLownloader: YoutubeDlDownloader

    lateinit var selector: DownloaderSelector

    @BeforeEach
    fun beforeEach() {
        val downloaders = setOf(ffmpegDownloader, rtmpDownloader, youtubeDLownloader)

        downloaders.forEach { whenever(it.compatibility(any())).thenCallRealMethod()}

        selector = DownloaderSelector(applicationContext, downloaders)
    }

    @Test
    fun `should reject empty url`() {
        /* When */
        assertThat(selector.of(DownloadingInformation(dItem, listOf(), Path("file.mp4"), null))).isEqualTo(DownloaderSelector.NO_OP_DOWNLOADER)
    }

    @MethodSource("urlToDownloader")
    @DisplayName("should return")
    @ParameterizedTest(name = "{0}")
    fun `should return matching downloader`(d: DownloaderArgument) {
        /* When */
        val finderClass = selector.of(d.item)
        /* Then */
        assertThat(finderClass).isInstanceOf(d.clazz.java)
    }

    companion object {
        @JvmStatic
        fun urlToDownloader(): Stream<DownloaderArgument> =
                Stream.of(
                        DownloaderArgument(URI.create("http://foo.bar.com/a/path/with/file.mp3"), YoutubeDlDownloader::class),
                        DownloaderArgument(URI.create("http://foo.bar.com/a/path/with/file.m3u8"), FfmpegDownloader::class),
                        DownloaderArgument(URI.create("rtmp://ma.video.free.fr/video.mp4/audio/tnt/tnt1217/tnt1217.mp3"), RTMPDownloader::class),
                        DownloaderArgument(URI.create("https://www.youtube.com/watch?v=RKh4T3m-Qlk&feature=youtube_gdata"), YoutubeDlDownloader::class)
                )
    }
}

private val dItem: DownloadingItem = DownloadingItem (
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


class DownloaderArgument(val url: URI, val clazz: KClass<*>) {
    val item = DownloadingInformation(dItem, listOf(url), Path("file.mp4"), null)

    override fun toString(): String {
        return "${clazz.simpleName} for $url"
    }
}
