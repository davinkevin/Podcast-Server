package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.downloader.FfmpegDownloader
import com.github.davinkevin.podcastserver.manager.downloader.HTTPDownloader
import com.github.davinkevin.podcastserver.manager.downloader.YoutubeDownloader
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.manager.downloader.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.stream.Stream
import kotlin.reflect.KClass

@ExtendWith(SpringExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DownloaderSelectorTest {

    @MockBean lateinit var httpDownloader: HTTPDownloader
    @MockBean lateinit var ffmpegDownloader: FfmpegDownloader
    @MockBean lateinit var rtmpDownloader: RTMPDownloader
    @MockBean lateinit var youtubeDownloader: YoutubeDownloader

    @Autowired lateinit var applicationContext: ApplicationContext
    lateinit var selector: DownloaderSelector

    @BeforeEach
    fun beforeEach() {
        val downloaders = setOf(httpDownloader, ffmpegDownloader, rtmpDownloader, youtubeDownloader)

        downloaders.forEach { whenever(it.compatibility(any())).thenCallRealMethod()}

        selector = DownloaderSelector(applicationContext, downloaders)
    }

    @Test
    fun `should reject empty url`() {
        /* When */
        assertThat(selector.of(DownloadingItem(null, listOf<String>().toVΛVΓ(), null, null))).isEqualTo(DownloaderSelector.NO_OP_DOWNLOADER)
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
        fun urlToDownloader() =
                Stream.of(
                        DownloaderArgument("http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/tnt/tnt1217/tnt1217.mp3", HTTPDownloader::class),
                        DownloaderArgument("http://foo.bar.com/a/path/with/file.m3u8", FfmpegDownloader::class),
                        DownloaderArgument("rtmp://ma.video.free.fr/video.mp4/audio/tnt/tnt1217/tnt1217.mp3", RTMPDownloader::class),
                        DownloaderArgument("https://www.youtube.com/watch?v=RKh4T3m-Qlk&feature=youtube_gdata", YoutubeDownloader::class)
                )
    }
}

class DownloaderArgument(val url: String, val clazz: KClass<*>) {
    val item = DownloadingItem(null, listOf(url).toVΛVΓ(), null, null)

    override fun toString(): String {
        return "${clazz.simpleName} for $url"
    }
}