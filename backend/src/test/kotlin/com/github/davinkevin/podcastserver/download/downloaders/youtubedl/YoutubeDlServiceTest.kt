package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDLException
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDLRequest
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDLResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import java.nio.file.Paths

@NestedSpringTest
@Import(YoutubeDlService::class)
class YoutubeDlServiceTest(
        @Autowired private val youtube: YoutubeDlService
) {

    @MockitoBean private lateinit var youtubeDl: YoutubeDL

    @BeforeEach
    fun beforeEach() = Mockito.reset(youtubeDl)

    @Nested
    @DisplayName("should identify video platforms")
    inner class ShouldIdentifyVideoPlatforms {

        @ParameterizedTest(name = "url {0}")
        @ValueSource(strings = [
            "https://youtube.com/file.mp3",
            "https://www.6play.fr/file.mp3",
            "https://www.tf1.fr/file.mp3",
            "https://www.france.tv/file.mp3",
            "https://replay.gulli.fr/file.mp3",
            "https://dailymotion.com/file.mp3"
        ])
        fun `with default hosts`(url: String) {
            assertThat(youtube.isFromVideoPlatform(url)).isTrue()
        }

        @Test
        fun `and treat any other url as a direct file`() {
            assertThat(youtube.isFromVideoPlatform("https://foo.bar.com/file.mp3")).isFalse()
        }

        @Test
        fun `with hosts customized by configuration`() {
            /* Given */
            val ytdlp = YoutubeDlService(youtubeDl, emptyMap(), videoPlatforms = listOf("my.platform.tv"))

            /* When & Then */
            assertThat(ytdlp.isFromVideoPlatform("https://my.platform.tv/video/123")).isTrue()
            assertThat(ytdlp.isFromVideoPlatform("https://youtube.com/file.mp3")).isFalse()
        }
    }

    @Nested
    @DisplayName("should extract name")
    inner class ShouldExtractName {

        private val url = "https://www.youtube.com/watch?v=48bK3mmjgRE"

        @ParameterizedTest(name = "{0} to {1}")
        @CsvSource(value = [
            "lowercase.mp3,lowercase.mp3",
            "UPPERCASE.mp3,UPPERCASE.mp3",
            "With0123456789.mp3,With0123456789.mp3",
            "with space.mp3,with_space.mp3",
            "with_accentuated_éé_chars.mp3,with_accentuated____chars.mp3",
            "with-special-chars-@-&-\"\'-!-§.mp3,with-special-chars-_-_-__-_-_.mp3"
        ])
        fun `with success`(name: String, transformed: String) {
            /* Given */
            val response = response(name)
            val requestForFileName = argForWhich<YoutubeDLRequest> {
                this.url == url &&
                  option["get-filename"] == "" &&
                  option["merge-output-format"] == "mp4"
            }
            whenever(youtubeDl.execute(requestForFileName)).thenReturn(response)

            /* When */
            val fileName = youtube.extractName(url)

            /* Then */
            assertThat(fileName).isEqualTo(transformed)
        }

        @Test
        fun `should use simple filename if not from video platform`() {
            /* Given */
            val itemUrl = "https://feeds.soundcloud.com/stream/1273036843-themarvelinitiative-hawkeye.mp3?with-parameter"
            /* When */
            val result = youtube.extractName(itemUrl)
            /* Then */
            assertThat(result).isEqualTo("1273036843-themarvelinitiative-hawkeye.mp3")
        }

        @Test
        fun `should rename hls manifest to mp4 because it is remuxed during download`() {
            /* Given */
            val itemUrl = "https://live.video.provider.com/stream/master.m3u8?token=123"
            /* When */
            val result = youtube.extractName(itemUrl)
            /* Then */
            assertThat(result).isEqualTo("master.mp4")
        }

        @Test
        fun `with error`() {
            /* Given */
            /* When */
            assertThatThrownBy { youtube.extractName(url) }
                    /* Then */
                    .hasMessage("Error during creation of filename of $url")
        }

        @Test
        fun `should use extra parameters provided by configuration`() {
            /* Given */
            val ytdlp = YoutubeDlService(youtubeDl, mapOf("foo" to "bar"))
            val response = response("lowercase.mp3")
            val requestForFileName = argForWhich<YoutubeDLRequest> {
                this.url == url && option["foo"] == "bar"
            }
            whenever(youtubeDl.execute(requestForFileName)).thenReturn(response)

            /* When */
            val fileName = ytdlp.extractName(url)

            /* Then */
            assertThat(fileName).isEqualTo("lowercase.mp3")
        }

        fun response(name: String) = YoutubeDLResponse(null, null, null, 0, 0, name, null)
    }

    @Nested
    @DisplayName("should download")
    inner class ShouldDownload {

        private val url = "https://foo.bar.com/file.mp3"
        private val destination = Paths.get("/tmp/", "foo.mp3")
        private val response = YoutubeDLResponse(null, null, null, 1, 1, null, null)
        private val progressCallback = DownloadProgressCallback { _ -> }

        @Test
        fun `should download http file`() {
            /* Given */
            val requestForDownload = argWhere<YoutubeDLRequest> {
                        it.url == url  &&
                        it.directory == "/tmp" &&
                        it.option["retries"] == "10" &&
                        it.option["output"] == "foo.mp3" &&
                        it.option["impersonate"] == "chrome"
            }
            whenever(youtubeDl.execute(requestForDownload, any(), anyOrNull())).thenReturn(response)

            /* When */
            val download = youtube.download(url, destination, progressCallback)

            /* Then */
            assertThat(download).isSameAs(response)
        }

        @Test
        fun `should download hls stream with impersonation and remux to mp4`() {
            /* Given */
            val hlsUrl = "https://live.video.provider.com/stream/master.m3u8"
            val requestForDownload = argWhere<YoutubeDLRequest> {
                        it.url == hlsUrl &&
                        it.directory == "/tmp" &&
                        it.option["retries"] == "10" &&
                        it.option["output"] == "foo.mp3" &&
                        it.option["impersonate"] == "chrome" &&
                        it.option["remux-video"] == "mp4" &&
                        !it.option.containsKey("format")
            }
            whenever(youtubeDl.execute(requestForDownload, any(), anyOrNull())).thenReturn(response)

            /* When */
            val download = youtube.download(hlsUrl, destination, progressCallback)

            /* Then */
            assertThat(download).isSameAs(response)
        }

        @Test
        fun `should not remux direct files which are not hls streams`() {
            /* Given */
            val requestForDownload = argWhere<YoutubeDLRequest> {
                it.url == url &&
                  !it.option.containsKey("remux-video")
            }
            whenever(youtubeDl.execute(requestForDownload, any(), anyOrNull())).thenReturn(response)

            /* When */
            val download = youtube.download(url, destination, progressCallback)

            /* Then */
            assertThat(download).isSameAs(response)
        }

        @Test
        fun `should not impersonate if disabled by configuration`() {
            /* Given */
            val ytdlp = YoutubeDlService(youtubeDl, emptyMap(), impersonate = "")
            val requestForDownload = argWhere<YoutubeDLRequest> {
                it.url == url &&
                  !it.option.containsKey("impersonate")
            }
            whenever(youtubeDl.execute(requestForDownload, any(), anyOrNull())).thenReturn(response)

            /* When */
            val download = ytdlp.download(url, destination, progressCallback)

            /* Then */
            assertThat(download).isSameAs(response)
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
        fun `from video platform with`(videoPlatformUrl: String) {
            /* Given */
            val requestForDownload = argWhere<YoutubeDLRequest> {
                        it.url == videoPlatformUrl  &&
                        it.directory == "/tmp" &&
                        it.option["retries"] == "10" &&
                        it.option["output"] == "foo.mp3" &&
                        it.option["merge-output-format"] == "mp4" &&
                        it.option["format"] == "bv+ba" &&
                        !it.option.containsKey("impersonate")
            }
            whenever(youtubeDl.execute(requestForDownload, any(), anyOrNull())).thenReturn(response)

            /* When */
            val download = youtube.download(videoPlatformUrl, destination, progressCallback)

            /* Then */
            assertThat(download).isSameAs(response)
        }

        @Test
        fun `should surface yt-dlp error lines when the download fails`() {
            /* Given */
            val stderr = """
                WARNING: unable to fetch something
                ERROR: HTTP Error 403: Forbidden
            """.trimIndent()
            whenever(youtubeDl.execute(any(), any(), anyOrNull())).thenThrow(YoutubeDLException(stderr))

            /* When */
            assertThatThrownBy { youtube.download(url, destination, progressCallback) }
                    /* Then */
                    .hasMessage("Error during download of $url: ERROR: HTTP Error 403: Forbidden")
        }

        @Test
        fun `should surface the last line when yt-dlp reports no explicit error`() {
            /* Given */
            whenever(youtubeDl.execute(any(), any(), anyOrNull()))
                .thenThrow(YoutubeDLException("first line\nsomething went wrong"))

            /* When */
            assertThatThrownBy { youtube.download(url, destination, progressCallback) }
                    /* Then */
                    .hasMessage("Error during download of $url: something went wrong")
        }

        @Test
        fun `should expose the started process to the caller`() {
            /* Given */
            val process = mock<Process>()
            var startedProcess: Process? = null
            whenever(youtubeDl.execute(any(), any(), anyOrNull())).then {
                it.getArgument<java.util.function.Consumer<Process>>(2).accept(process)
                response
            }

            /* When */
            youtube.download(url, destination, progressCallback) { startedProcess = it }

            /* Then */
            assertThat(startedProcess).isSameAs(process)
        }

        @Test
        fun `should call callback to propagate progression`() {
            /* Given */
            val captor = argumentCaptor<DownloadProgressCallback>()
            var isCalled = false
            val changeValue = DownloadProgressCallback { _ -> isCalled = true}

            whenever(youtubeDl.execute(any(), any(), anyOrNull())).thenReturn(response)
            youtube.download(url, destination, changeValue)
            verify(youtubeDl).execute(any(), captor.capture(), anyOrNull())

            /* When */
            captor.firstValue.onProgressUpdate(1f)

            /* Then */
            assertThat(isCalled).isTrue
        }

        @Test
        fun `should use extra parameters provided by configuration`() {
            /* Given */
            val ytdlp = YoutubeDlService(youtubeDl, mapOf("foo" to "bar"))
            val requestForDownload = argWhere<YoutubeDLRequest> {
                it.url == "https://youtube.com/file.mp3"  &&
                  it.directory == "/tmp" &&
                  it.option["retries"] == "10" &&
                  it.option["output"] == "foo.mp3" &&
                  it.option["merge-output-format"] == "mp4" &&
                  it.option["format"] == "bv+ba"
                  it.option["foo"] == "bar"
            }
            whenever(youtubeDl.execute(requestForDownload, any(), anyOrNull())).thenReturn(response)

            /* When */
            val download = ytdlp.download(
                url = "https://youtube.com/file.mp3",
                destination = destination,
                callback = progressCallback
            )

            /* Then */
            assertThat(download).isSameAs(response)
        }

    }

}
