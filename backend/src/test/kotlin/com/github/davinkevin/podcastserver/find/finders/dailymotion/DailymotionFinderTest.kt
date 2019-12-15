package com.github.davinkevin.podcastserver.find.finders.dailymotion

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nhaarman.mockitokotlin2.any
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

@ExtendWith(SpringExtension::class)
class DailymotionFinderTest(
        @Autowired val image: ImageService,
        @Autowired val finder: DailymotionFinder
) {

    @Nested
    @DisplayName("should find")
    inner class ShouldFind {

        @Nested
        @ExtendWith(MockServer::class)
        @DisplayName("with success")
        inner class WithSuccess {

            @Test
            fun `information about dailymotion podcast with url`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.dailymotion.com/karimdebbache"

                whenever(image.fetchCoverInformation(URI("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg")))
                        .thenReturn(CoverInformation(
                                url = URI("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg"),
                                height = 123,
                                width = 456
                        ).toMono())

                backend.stubFor(get(urlPathEqualTo("/user/karimdebbache"))
                        .withQueryParam("fields", equalTo("avatar_720_url,description,username"))
                        .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.json"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.url).isEqualTo(URI(url))
                            assertThat(it.title).isEqualTo("karimdebbache")
                            assertThat(it.type).isEqualTo("Dailymotion")
                            assertThat(it.cover).isEqualTo(FindCoverInformation(
                                    url = URI("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg"),
                                    height = 123,
                                    width = 456
                            ))
                            assertThat(it.description).isEqualTo("""CHROMA est une CHROnique de cinéMA sur Dailymotion, dont la première saison se compose de dix épisodes, à raison d’un par mois, d’une durée comprise entre quinze et vingt minutes. Chaque épisode est consacré à un film en particulier.""")
                        }
                        .verifyComplete()
            }

            @Test
            fun `information about dailymotion podcast with url but with no cover`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.dailymotion.com/karimdebbache"

                whenever(image.fetchCoverInformation(any())).thenReturn(Mono.empty())

                backend.stubFor(get(urlPathEqualTo("/user/karimdebbache"))
                        .withQueryParam("fields", equalTo("avatar_720_url,description,username"))
                        .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.json"))))

                /* When */
                StepVerifier.create(finder.findInformation(url))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.url).isEqualTo(URI(url))
                            assertThat(it.title).isEqualTo("karimdebbache")
                            assertThat(it.type).isEqualTo("Dailymotion")
                            assertThat(it.cover).isNull()
                            assertThat(it.description).isEqualTo("""CHROMA est une CHROnique de cinéMA sur Dailymotion, dont la première saison se compose de dix épisodes, à raison d’un par mois, d’une durée comprise entre quinze et vingt minutes. Chaque épisode est consacré à un film en particulier.""")
                        }
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("with error")
        inner class WithError {

            @Test
            fun `if username can't be extracted from url`() {
                /* Given */
                val url = "https://www.toto.com/karimdebbache"
                /* When */
                StepVerifier.create(finder.findInformation(url))
                /* Then */
                        .expectSubscription()
                        .expectErrorMessage("username not found int url $url")
                        .verify()
            }

        }

    }


    @DisplayName("shoud be compatible")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "https://www.dailymotion.com/karimdebbache/",
        "http://www.dailymotion.com/karimdebbache/"
    ])
    fun `should be compatible with`(/* Given */ url: String) {
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
        "https://www.6play.fr/france-2/vu/"
    ])
    fun `should not be compatible`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
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
    @Import(DailymotionFinderConfig::class)
    class LocalTestConfiguration {

        @Bean @Primary fun imageService() = mock<ImageService>()
        @Bean fun webClientBuilder() = WebClient.builder()
                .filter(remapToMockServer("api.dailymotion.com"))
    }

}
