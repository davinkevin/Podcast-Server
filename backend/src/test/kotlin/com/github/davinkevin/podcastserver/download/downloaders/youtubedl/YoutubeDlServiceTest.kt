package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
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
import java.nio.file.Paths

@ExtendWith(SpringExtension::class)
@Import(YoutubeDlService::class)
class YoutubeDlServiceTest(
        @Autowired private val youtube: YoutubeDlService
) {

    @MockitoBean private lateinit var youtubeDl: YoutubeDL

    @BeforeEach
    fun beforeEach() = Mockito.reset(youtubeDl)

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
                        it.option["output"] == "foo.mp3"
            }
            whenever(youtubeDl.execute(requestForDownload, any())).thenReturn(response)

            /* When */
            val download = youtube.download(url, destination, progressCallback)

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
                        it.option["format"] == "bv+ba"
            }
            whenever(youtubeDl.execute(requestForDownload, any())).thenReturn(response)

            /* When */
            val download = youtube.download(videoPlatformUrl, destination, progressCallback)

            /* Then */
            assertThat(download).isSameAs(response)
        }

        @Test
        fun `should call callback to propagate progression`() {
            /* Given */
            val captor = argumentCaptor<DownloadProgressCallback>()
            var isCalled = false
            val changeValue = DownloadProgressCallback { _ -> isCalled = true}

            whenever(youtubeDl.execute(any(), any())).thenReturn(response)
            youtube.download(url, destination, changeValue)
            verify(youtubeDl).execute(any(), captor.capture())

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
            whenever(youtubeDl.execute(requestForDownload, any())).thenReturn(response)

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
