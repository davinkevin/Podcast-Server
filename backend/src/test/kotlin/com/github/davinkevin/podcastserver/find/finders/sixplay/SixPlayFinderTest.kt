package com.github.davinkevin.podcastserver.find.finders.sixplay

import com.github.davinkevin.podcastserver.*
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService


@ExtendWith(SpringExtension::class)
class SixPlayFinderTest(
        @Autowired val image: ImageService,
        @Autowired val finder: SixPlayFinder
) {

    @Nested
    @ExtendWith(MockServer::class)
    @DisplayName("should find")
    inner class ShouldFind {

        @Test
        fun `information about 6Play podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.6play.fr/sport-6-p_1380"
            val coverUrl = URI("https://images.6play.fr/v2/images/598896/raw?width=1024&height=576&fit=max&quality=60&format=jpeg&interlace=1&hash=33abccc9be94554a7081bb8cc10a1fe94b8fefa0")

            whenever(image.fetchCoverInformation(coverUrl)).thenReturn(CoverInformation(
                    url = coverUrl,
                    height = 123,
                    width = 456
            ).toMono())

            backend.stubFor(get("/sport-6-p_1380")
                    .willReturn(ok(fileAsString("/remote/podcast/6play/sport-6-p_1380.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .expectNext(FindPodcastInformation(
                            title = "Sport 6",
                            type = "SixPlay",
                            description = "Retrouvez chaque semaine toute l'actualité et les résultats du sport dans Sport 6. En 6 minutes, priorités aux images : les temps forts de l'actualité et les résultats sportifs sont décryptés pour tout connaître des faits marquants de la semaine.",
                            url = URI(url),
                            cover = FindCoverInformation(
                                    url = coverUrl,
                                    height = 123,
                                    width = 456
                            )
                    ))
                    .verifyComplete()
        }

        @Nested
        @DisplayName("without description")
        inner class WithSpecificDescription {

            private val url = "https://www.6play.fr/sport-6-p_1380"
            private val coverUrl = URI("https://images.6play.fr/v2/images/598896/raw?width=1024&height=576&fit=max&quality=60&format=jpeg&interlace=1&hash=33abccc9be94554a7081bb8cc10a1fe94b8fefa0")

            @BeforeEach
            fun beforeEach() {
                whenever(image.fetchCoverInformation(coverUrl)).thenReturn(CoverInformation(
                        url = coverUrl,
                        height = 123,
                        width = 456
                ).toMono())
            }

            @Test
            fun `because script tag is missing`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/sport-6-p_1380")
                        .willReturn(ok(fileAsString("/remote/podcast/6play/sport-6-p_1380-without-js.html"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Sport 6",
                                type = "SixPlay",
                                description = "",
                                url = URI(url),
                                cover = FindCoverInformation(
                                        url = coverUrl,
                                        height = 123,
                                        width = 456
                                )
                        ))
                        .verifyComplete()
            }

            @Test
            fun `because script tag doesnt have program`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/sport-6-p_1380")
                        .willReturn(ok(fileAsString("/remote/podcast/6play/sport-6-p_1380-without-programid.html"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Sport 6",
                                type = "SixPlay",
                                description = "",
                                url = URI(url),
                                cover = FindCoverInformation(
                                        url = coverUrl,
                                        height = 123,
                                        width = 456
                                )
                        ))
                        .verifyComplete()
            }

            @Test
            fun `because programsById 1300 doesnt have description`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/sport-6-p_1380")
                        .willReturn(ok(fileAsString("/remote/podcast/6play/sport-6-p_1380-without-description.html"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Sport 6",
                                type = "SixPlay",
                                description = "",
                                url = URI(url),
                                cover = FindCoverInformation(
                                        url = coverUrl,
                                        height = 123,
                                        width = 456
                                )
                        ))
                        .verifyComplete()
            }

            @Test
            fun `because programsById is empty`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/sport-6-p_1380")
                        .willReturn(ok(fileAsString("/remote/podcast/6play/sport-6-p_1380.without-entries-in-programs-by-id.html"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Sport 6",
                                type = "SixPlay",
                                description = "",
                                url = URI(url),
                                cover = FindCoverInformation(
                                        url = coverUrl,
                                        height = 123,
                                        width = 456
                                )
                        ))
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("without cover")
        inner class WithSpecificCover {

            private val url = "https://www.6play.fr/sport-6-p_1380"

            @Test
            fun `because no background image is defined`(backend: WireMockServer) {
                /* Given */
                backend.stubFor(get("/sport-6-p_1380")
                        .willReturn(ok(fileAsString("/remote/podcast/6play/sport-6-p_1380.without-cover.html"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .expectNext(FindPodcastInformation(
                                title = "Sport 6",
                                type = "SixPlay",
                                description = "Retrouvez chaque semaine toute l'actualité et les résultats du sport dans Sport 6. En 6 minutes, priorités aux images : les temps forts de l'actualité et les résultats sportifs sont décryptés pour tout connaître des faits marquants de la semaine.",
                                url = URI(url),
                                cover = null
                        ))
                        .verifyComplete()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://www.6play.fr/scenes-de-menages-p_829",
        "https://www.6play.fr/sport-6-p_1380",
        "https://www.6play.fr/le-meilleur-patissier-p_1807",
        "https://www.6play.fr/barbie-dreamhouse-adventures-p_15205"
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
        "https://www.gulli.fr/france-2/vu/"
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
    @Import(SixPlayFinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean @Primary fun imageService() = mock<ImageService>()
        @Bean fun webClientCustomization() = WebClientCustomizer { it.filter(remapToMockServer("www.6play.fr")) }
    }

}
