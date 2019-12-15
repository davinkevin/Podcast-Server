package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

@ExtendWith(SpringExtension::class)
class YoutubeFinderTest(
    @Autowired val imageService: ImageService,
    @Autowired val finder: YoutubeFinder
) {

    @Nested
    @DisplayName("should find")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        @Test
        fun `information about a youtube podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "http://localhost:5555/user/cauetofficiel"
            val coverUrl = URI("https://yt3.ggpht.com/-83tzNbjW090/AAAAAAAAAAI/AAAAAAAAAAA/Vj6_1jPZOVc/s1400-c-k-no/photo.jpg")
            whenever(imageService.fetchCoverInformation(coverUrl))
                    .thenReturn(CoverInformation(100, 100, coverUrl).toMono())

            backend.stubFor(get("/user/cauetofficiel")
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/youtube.cauetofficiel.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { podcast ->
                        assertThat(podcast.title).isEqualTo("Cauet")
                        assertThat(podcast.description).isEqualTo("La chaîne officielle de Cauet, c'est toujours plus de kiff et de partage ! Des vidéos exclusives de C'Cauet sur NRJ tous les soirs de 19h à 22h. Des défis in...")

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
            val url = "http://localhost:5555/user/cauetofficiel"
            backend.stubFor(get("/user/cauetofficiel")
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/youtube.cauetofficiel.withoutDescAndCoverAndTitle.html"))))

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

    @Test
    fun `should do nothing on old implementation`() {
        /* Given */
        /* When */
        Assertions.assertThatThrownBy { finder.find("") }
                /* Then */
                .hasMessage("An operation is not implemented: not required anymore")
    }

    @TestConfiguration
    @Import(YoutubeFinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun imageService() = mock<ImageService>()
    }
}
