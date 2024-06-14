package com.github.davinkevin.podcastserver.find.finders.francetv

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 01/11/2019
 */
@AutoConfigureWebClient
@ExtendWith(SpringExtension::class)
class FranceTvFinderTest(
    @Autowired val finder: FranceTvFinder
) {

    @MockBean lateinit var image: ImageService

    @Nested
    @ExtendWith(MockServer::class)
    @DisplayName("should find")
    inner class ShouldFind {

        @Test
        fun `information about france tv podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.france.tv/france-3/secrets-d-histoire/"

            whenever(image.fetchCoverInformation(any())).thenReturn(CoverInformation(
                url = URI("https://foo.bar.com"),
                height = 123,
                width = 456
            ).toMono())

            backend.stubFor(get("/france-3/secrets-d-histoire/")
                .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/secrets-d-histoire.html"))))

            /* When */
            val podcast = finder.findPodcastInformation(url)!!

            /* Then */
            assertAll {
                assertThat(podcast.url).isEqualTo(URI(url))
                assertThat(podcast.title).isEqualTo("Secrets d'Histoire")
                assertThat(podcast.type).isEqualTo("FranceTv")
                assertThat(podcast.cover).isEqualTo(FindCoverInformation(
                    url = URI("https://foo.bar.com"),
                    height = 123,
                    width = 456
                ))
                assertThat(podcast.description).isEqualTo("""Secrets d'Histoire est une émission de télévision présentée par Stéphane Bern. Chaque numéro retrace la vie d'un grand personnage de l'histoire et met en lumière des lieux hautement emblématiques du patrimoine. Magazine Secrets d'Histoire Accessible à tous, le magazine Secrets d’Histoire vous entraîne au cœur des épisodes mystérieux de l’histoire à travers des reportages, des enquêtes, des quizz… et bien plus encore ! En savoir plus""")
            }
        }

        @Test
        fun `information about france tv podcast with its url with no cover`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.france.tv/france-3/secrets-d-histoire/"

            whenever(image.fetchCoverInformation(any())).thenReturn(Mono.empty())
            backend.stubFor(get("/france-3/secrets-d-histoire/")
                .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/secrets-d-histoire.html"))))

            /* When */
            val podcast = finder.findPodcastInformation(url)!!
            /* Then */
            assertAll {
                assertThat(podcast.url).isEqualTo(URI(url))
                assertThat(podcast.title).isEqualTo("Secrets d'Histoire")
                assertThat(podcast.type).isEqualTo("FranceTv")
                assertThat(podcast.cover).isNull()
                assertThat(podcast.description).isEqualTo("""Secrets d'Histoire est une émission de télévision présentée par Stéphane Bern. Chaque numéro retrace la vie d'un grand personnage de l'histoire et met en lumière des lieux hautement emblématiques du patrimoine. Magazine Secrets d'Histoire Accessible à tous, le magazine Secrets d’Histoire vous entraîne au cœur des épisodes mystérieux de l’histoire à travers des reportages, des enquêtes, des quizz… et bien plus encore ! En savoir plus""")
            }
        }

        @Test
        fun `no podcast and return null if page is empty`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.france.tv/france-3/secrets-d-histoire/"

            whenever(image.fetchCoverInformation(any())).thenReturn(CoverInformation(
                url = URI("https://foo.bar.com"),
                height = 123,
                width = 456
            ).toMono())

            backend.stubFor(get("/france-3/secrets-d-histoire/")
                .willReturn(ok()))

            /* When */
            val podcast = finder.findPodcastInformation(url)

            /* Then */
            assertThat(podcast).isNull()
        }
    }

    @DisplayName("shoud be compatible")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "https://www.france.tv/france-2/vu/",
        "https://www.france.tv/france-2/secrets-d-histoire/",
        "https://www.france.tv/france-3/agatha-raisin/",
        "https://www.france.tv/france-2/parents-mode-d-emploi/"
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
        FranceTvFinderConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
        WebClientConfig::class
    )
    class LocalTestConfiguration {
        @Bean fun remapToMockServer() = remapRestClientToMockServer("www.france.tv")
    }
}
