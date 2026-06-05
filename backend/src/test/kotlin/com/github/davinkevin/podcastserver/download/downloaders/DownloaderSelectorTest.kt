package com.github.davinkevin.podcastserver.download.downloaders

import com.github.davinkevin.podcastserver.download.downloaders.youtubedl.YoutubeDlDownloaderFactory
import com.github.davinkevin.podcastserver.download.downloaders.youtubedl.YoutubeDlService
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import java.net.URI
import java.util.*
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.reflect.KClass

@NestedSpringTest
class DownloaderSelectorTest(
    @Autowired val applicationContext: ApplicationContext
) {

    private val youtubeDLDownloader = YoutubeDlDownloaderFactory(mock(), YoutubeDlService(mock(), emptyMap()))

    lateinit var selector: DownloaderSelector

    @BeforeEach
    fun beforeEach() {
        selector = DownloaderSelector(applicationContext, setOf(youtubeDLDownloader))
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
                        DownloaderArgument(URI.create("http://foo.bar.com/a/path/with/file.mp3"), YoutubeDlDownloaderFactory::class),
                        DownloaderArgument(URI.create("http://foo.bar.com/a/path/with/file.m3u8"), YoutubeDlDownloaderFactory::class),
                        DownloaderArgument(URI.create("http://foo.bar.com/a/path/with/file.mp4"), YoutubeDlDownloaderFactory::class),
                        DownloaderArgument(URI.create("https://www.youtube.com/watch?v=RKh4T3m-Qlk&feature=youtube_gdata"), YoutubeDlDownloaderFactory::class)
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
    val item = DownloadingInformation(dItem, url, Path("file.mp4"))

    override fun toString(): String {
        return "${clazz.simpleName} for $url"
    }
}
