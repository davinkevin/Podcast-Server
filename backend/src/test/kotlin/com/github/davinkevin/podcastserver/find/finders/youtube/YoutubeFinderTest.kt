package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.tomakehurst.wiremock.client.WireMock.*

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
            val coverUrl = URI("https://yt3.ggpht.com/a/AATXAJzzJHBMXD4K4L5FX4X_TnfKO16Wy9M4pzlshph5=s900-c-k-c0xffffffff-no-rj-mo")

            whenever(image.fetchCoverInformation(coverUrl))
                    .thenReturn(CoverInformation(100, 100, coverUrl).toMono())

            backend.stubFor(get("/user/joueurdugrenier")
                    .withHeader("User-Agent", equalTo("curl/7.64.1"))
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { podcast ->
                        assertThat(podcast.title).isEqualTo("Joueur Du Grenier")
                        assertThat(podcast.description).isEqualTo("Test de jeux à la con !")

                        assertThat(podcast.cover).isNotNull
                        assertThat(podcast.cover!!.url).isEqualTo(coverUrl)
                    }
                    .verifyComplete()
        }

        @Test
        fun `information about a youtube podcast with its url after redirect`(backend: WireMockServer) {
            /* Given */
            val url = "http://localhost:5555/user/joueurdugrenier"
            val coverUrl = URI("https://yt3.ggpht.com/a/AATXAJzzJHBMXD4K4L5FX4X_TnfKO16Wy9M4pzlshph5=s900-c-k-c0xffffffff-no-rj-mo")

            whenever(image.fetchCoverInformation(coverUrl))
                    .thenReturn(CoverInformation(100, 100, coverUrl).toMono())

            backend.apply {
                stubFor(get("/user/joueurdugrenier")
                        .withHeader("User-Agent", equalTo("curl/7.64.1"))
                        .willReturn(permanentRedirect("http://localhost:5555/user/joueurdugrenier-after-redirect")))

                stubFor(get("/user/joueurdugrenier-after-redirect")
                        .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))

            }

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { podcast ->
                        assertThat(podcast.title).isEqualTo("Joueur Du Grenier")
                        assertThat(podcast.description).isEqualTo("Test de jeux à la con !")

                        assertThat(podcast.cover).isNotNull
                        assertThat(podcast.cover!!.url).isEqualTo(coverUrl)
                    }
                    .verifyComplete()
        }


        @Test
        fun `should not find podcast for this url`() {
            /* Given */
            /* When */
            StepVerifier.create(finder.findInformation("http://localhost:5566/foo/bar"))
                    /* Then */
                    .expectSubscription()
                    .expectError()
                    .verify()
        }

        @Test
        fun `should set default value for information not found`(backend: WireMockServer) {
            /* Given */
            val url = "http://localhost:5555/user/joueurdugrenier"
            backend.stubFor(get("/user/joueurdugrenier")
                    .withHeader("User-Agent", equalTo("curl/7.64.1"))
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.withoutDescAndCoverAndTitle.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { podcast ->
                        assertThat(podcast.title).isEmpty()
                        assertThat(podcast.description).isEmpty()
                        assertThat(podcast.cover).isNull()
                    }
                    .verifyComplete()
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
    @Import(YoutubeFinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration
}
