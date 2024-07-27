package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

@ExtendWith(SpringExtension::class)
class YoutubeFinderTest(
    @Autowired val finder: YoutubeFinder
) {

    @MockBean lateinit var image: ImageService

    @Nested
    @DisplayName("should find")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        @Test
        fun `information about a youtube podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "http://localhost:5555/user/joueurdugrenier"
            val coverUrl = URI("https://yt3.ggpht.com/ytc/AAUvwnhJmYkW42zA0rx8V37HS_MbK_IX09HKCwaIsuU-=s900-c-k-c0x00ffffff-no-rj")

            whenever(image.fetchCoverInformation(coverUrl))
                .thenReturn(CoverInformation(100, 100, coverUrl))

            backend.stubFor(get("/user/joueurdugrenier")
                .withHeader("User-Agent", equalTo("curl/7.64.1"))
                .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.html")))
            )

            /* When */
            val podcast = finder.findPodcastInformation(url)!!

            /* Then */
            assertAll {
                assertThat(podcast.title).isEqualTo("Joueur Du Grenier")
                assertThat(podcast.description).isEqualTo("Test de jeux à la con !")

                assertThat(podcast.cover).isNotNull
                assertThat(podcast.cover!!.url).isEqualTo(coverUrl)
            }
        }

        @Test
        fun `information about a youtube podcast with its url after redirect`(backend: WireMockServer) {
            /* Given */
            val url = "http://localhost:5555/user/joueurdugrenier"
            val coverUrl = URI("https://yt3.ggpht.com/ytc/AAUvwnhJmYkW42zA0rx8V37HS_MbK_IX09HKCwaIsuU-=s900-c-k-c0x00ffffff-no-rj")

            whenever(image.fetchCoverInformation(coverUrl))
                .thenReturn(CoverInformation(100, 100, coverUrl))

            backend.apply {
                stubFor(get("/user/joueurdugrenier")
                    .withHeader("User-Agent", equalTo("curl/7.64.1"))
                    .willReturn(permanentRedirect("http://localhost:5555/user/joueurdugrenier-after-redirect")))

                stubFor(get("/user/joueurdugrenier-after-redirect")
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))

            }

            /* When */
            val podcast = finder.findPodcastInformation(url)!!

            /* Then */
            assertAll {
                assertThat(podcast.title).isEqualTo("Joueur Du Grenier")
                assertThat(podcast.description).isEqualTo("Test de jeux à la con !")

                assertThat(podcast.cover).isNotNull
                assertThat(podcast.cover!!.url).isEqualTo(coverUrl)
            }
        }


        @Test
        fun `should not find podcast for this url`() {
            assertThatThrownBy {
                finder.findPodcastInformation("http://localhost:3578")!!
            }
        }

        @Test
        fun `should set default value for information not found`(backend: WireMockServer) {
            /* Given */
            val url = "http://localhost:5555/user/joueurdugrenier"
            backend.stubFor(get("/user/joueurdugrenier")
                .withHeader("User-Agent", equalTo("curl/7.64.1"))
                .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.withoutDescAndCoverAndTitle.html"))))

            /* When */
            val podcast = finder.findPodcastInformation(url)!!

            /* Then */
            assertAll {
                assertThat(podcast.title).isEmpty()
                assertThat(podcast.description).isEmpty()
                assertThat(podcast.cover).isNull()
            }
        }

    }

    @DisplayName("should be compatible with")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "http://www.youtube.com/channel/a-channel", "http://youtube.com/user/foo-User",
        "https://gdata.youtube.com/feeds/api/playlists/UE1987158913731", "https://another.youtube.com/bar-foo"
    ])
    fun `should be compatible with`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "http://foo.bar.com/"
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    @TestConfiguration
    @Import(
        YoutubeFinderConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration
}
