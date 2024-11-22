package com.github.davinkevin.podcastserver.find.finders.dailymotion

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

@ExtendWith(SpringExtension::class)
class DailymotionFinderTest(
        @Autowired val finder: DailymotionFinder
) {

    @MockitoBean lateinit var image: ImageService

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
                        ))

                backend.stubFor(get(urlPathEqualTo("/user/karimdebbache"))
                        .withQueryParam("fields", equalTo("avatar_720_url,description,username"))
                        .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.json"))))

                /* When */
                val podcast = finder.findPodcastInformation(url)!!

                /* Then */
                assertAll {
                    assertThat(podcast.url).isEqualTo(URI(url))
                    assertThat(podcast.title).isEqualTo("karimdebbache")
                    assertThat(podcast.type).isEqualTo("Dailymotion")
                    assertThat(podcast.cover).isEqualTo(FindCoverInformation(
                        url = URI("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg"),
                        height = 123,
                        width = 456
                    ))
                    assertThat(podcast.description).isEqualTo("""CHROMA est une CHROnique de cinéMA sur Dailymotion, dont la première saison se compose de dix épisodes, à raison d’un par mois, d’une durée comprise entre quinze et vingt minutes. Chaque épisode est consacré à un film en particulier.""")
                }
            }

            @Test
            fun `information about dailymotion podcast with url but with no cover`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.dailymotion.com/karimdebbache"

                whenever(image.fetchCoverInformation(any())).thenReturn(null)

                backend.stubFor(get(urlPathEqualTo("/user/karimdebbache"))
                        .withQueryParam("fields", equalTo("avatar_720_url,description,username"))
                        .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.json"))))

                /* When */
                val podcast = finder.findPodcastInformation(url)!!
                /* Then */
                assertAll {
                    assertThat(podcast.url).isEqualTo(URI(url))
                    assertThat(podcast.title).isEqualTo("karimdebbache")
                    assertThat(podcast.type).isEqualTo("Dailymotion")
                    assertThat(podcast.cover).isNull()
                    assertThat(podcast.description).isEqualTo("""CHROMA est une CHROnique de cinéMA sur Dailymotion, dont la première saison se compose de dix épisodes, à raison d’un par mois, d’une durée comprise entre quinze et vingt minutes. Chaque épisode est consacré à un film en particulier.""")
                }
            }

            @Test
            fun `information about dailymotion podcast with no description`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.dailymotion.com/karimdebbache"

                whenever(image.fetchCoverInformation(any())).thenReturn(null)

                backend.stubFor(get(urlPathEqualTo("/user/karimdebbache"))
                        .withQueryParam("fields", equalTo("avatar_720_url,description,username"))
                        .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache-without-description.json"))))

                /* When */
                val podcast = finder.findPodcastInformation(url)!!

                /* Then */
                assertAll {
                    assertThat(podcast.url).isEqualTo(URI(url))
                    assertThat(podcast.title).isEqualTo("karimdebbache")
                    assertThat(podcast.type).isEqualTo("Dailymotion")
                    assertThat(podcast.description).isEqualTo("")
                }
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
                assertThatThrownBy { finder.findPodcastInformation(url) }
                /* Then */
                    .hasMessageContaining("username not found in url $url")
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

    @TestConfiguration
    @Import(
        DailymotionFinderConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration {
        @Bean fun remapApiDailyMotion() = remapRestClientToMockServer("api.dailymotion.com")
    }

}
