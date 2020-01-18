package com.github.davinkevin.podcastserver.find.finders.mycanal

import com.github.davinkevin.podcastserver.*
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI

/**
 * Created by kevin on 03/11/2019
 */
@ExtendWith(SpringExtension::class)
class MyCanalFinderTest (
        @Autowired val finder: MyCanalFinder
) {

    @MockBean lateinit var image: ImageServiceV2

    @Nested
    @DisplayName("should find")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        @Nested
        @DisplayName("with success")
        inner class WithSuccess {

            @Test
            fun `podcast by url with cover`(backend: WireMockServer) {
                /* Given */
                val podcastUrl = "https://www.canalplus.com/theme/emissions/pid1323-canal-football-club.html"
                val coverUrl = URI("https://thumb.canalplus.pro/bran/unsafe/1920x665/top/image/55/8/mycanal_cover_logotypeee_1920x665.42558.jpg")

                whenever(image.fetchCoverInformation(coverUrl))
                        .thenReturn(CoverInformation(123, 456, coverUrl).toMono())

                backend.stubFor(get("/theme/emissions/pid1323-canal-football-club.html")
                        .willReturn(ok(fileAsString("/remote/podcast/mycanal/finder/pid1323-canal-football-club.html")))
                )

                /* When */
                StepVerifier.create(finder.findInformation(podcastUrl))
                        /* Then */
                        .expectSubscription()
                        .assertNextEqual(FindPodcastInformation(
                                title = "CANAL Football Club",
                                url = URI("https://www.canalplus.com/theme/emissions/pid1323-canal-football-club.html"),
                                type = "MyCanal",
                                description = "",
                                cover = FindCoverInformation(
                                        width = 123,
                                        height = 456,
                                        url = coverUrl
                                )
                        ))
                        .verifyComplete()
            }

            @Test
            fun `podcast by url without cover`(backend: WireMockServer) {
                /* Given */
                val podcastUrl = "https://www.canalplus.com/theme/emissions/pid1323-canal-football-club.html"

                backend.stubFor(get("/theme/emissions/pid1323-canal-football-club.html")
                        .willReturn(ok(fileAsString("/remote/podcast/mycanal/finder/with-no-cover-in-landing.html")))
                )

                /* When */
                StepVerifier.create(finder.findInformation(podcastUrl))
                        /* Then */
                        .expectSubscription()
                        .assertNextEqual(FindPodcastInformation(
                                title = "CANAL Football Club",
                                url = URI("https://www.canalplus.com/theme/emissions/pid1323-canal-football-club.html"),
                                type = "MyCanal",
                                description = "",
                                cover = null
                        ))
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("with error")
        inner class WithError {

            private val podcastUrl = "https://www.canalplus.com/theme/emissions/pid1323-canal-football-club.html"

            @Test
            fun `because no script tag with app_config`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/theme/emissions/pid1323-canal-football-club.html")
                        .willReturn(ok(fileAsString("/remote/podcast/mycanal/finder/without-app-config.html")))
                )
                /* When */
                StepVerifier.create(finder.findInformation(podcastUrl))
                        /* Then */
                        .expectSubscription()
                        .expectErrorSatisfies {
                            assertThat(it).isInstanceOf(RuntimeException::class.java)
                                    .hasMessage("app_config not found")
                        }
                        .verify()
            }

            @Test
            fun `because script tag has its structure changed due to lack of __data`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/theme/emissions/pid1323-canal-football-club.html")
                        .willReturn(ok(fileAsString("/remote/podcast/mycanal/finder/with-app-structure-changed.html")))
                )
                /* When */
                StepVerifier.create(finder.findInformation(podcastUrl))
                        /* Then */
                        .expectSubscription()
                        .expectErrorSatisfies {
                            assertThat(it).isInstanceOf(RuntimeException::class.java)
                                    .hasMessage("app_config has change its structure")
                        }
                        .verify()
            }

            @Test
            fun `because script tag has its structure changed due to lack of end bracked`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/theme/emissions/pid1323-canal-football-club.html")
                        .willReturn(ok(fileAsString("/remote/podcast/mycanal/finder/with-no-ending-bracket.html")))
                )
                /* When */
                StepVerifier.create(finder.findInformation(podcastUrl))
                        /* Then */
                        .expectSubscription()
                        .expectErrorSatisfies {
                            assertThat(it).isInstanceOf(RuntimeException::class.java)
                                    .hasMessage("app_config has change its structure")
                        }
                        .verify()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://www.canalplus.com/canal-football-club/h/cplusald_them_CFC15",
        "https://www.canalplus.com/chaines/clique-tv",
        "https://www.canalplus.com/c8/tpmp/touche-pas-a-mon-poste",
        "https://www.canalplus.com/sport/on-board-f-1/h/11319022_50001"
    ])
    fun `should be compatible with `(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @DisplayName("shoud not be compatible")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "https://www.france2.tv/france-2/vu/",
        "https://www.foo.com/france-2/vu/",
        "https://www.mycanal.fr/france-2/vu/",
        "https://www.gulli.fr/france-2/vu/",
        "https://www.6play.fr/barbie-dreamhouse-adventures-p_15205",
        ""
    ])
    fun `should not be compatible`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `should not be compatible with null value`() {
        /* Given */
        /* When */
        val compatibility = finder.compatibility(null)
        /* Then */
        assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `should do nothing on old implementation`() {
        /* Given */
        /* When */
        assertThatThrownBy { finder.find("") }
                /* Then */
                .hasMessage("An operation is not implemented: not required anymore")
    }

    @TestConfiguration
    @Import(MyCanalFinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun webClientCustomization() = WebClientCustomizer { it.filter(remapToMockServer("www.canalplus.com")) }
    }
}

private fun <T> StepVerifier.Step<T>.assertNextEqual(value: T) = this.assertNext {
    assertThat(it).isEqualTo(value)
}
