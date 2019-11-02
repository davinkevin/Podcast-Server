package com.github.davinkevin.podcastserver.find.finders.francetv

import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 01/11/2019
 */
@AutoConfigureWebClient
@ExtendWith(SpringExtension::class)
class FranceTvFinderTest(
        @Autowired val imageService: ImageService,
        @Autowired val finder: FranceTvFinder
) {

    @Nested
    @ExtendWith(MockServer::class)
    @DisplayName("should find")
    inner class ShouldFind {

        @Test
        fun `information about france tv podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.france.tv/france-2/secrets-d-histoire/"

            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(
                    url = URI("https://foo.bar.com"),
                    height = 123,
                    width = 456
            ).toMono())

            backend.stubFor(get("/france-2/secrets-d-histoire/")
                    .willReturn(ok(IOUtils.fileAsString("/remote/podcast/francetv/secrets-d-histoire.home.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.url).isEqualTo(URI(url))
                        assertThat(it.title).isEqualTo("Secrets d'Histoire")
                        assertThat(it.type).isEqualTo("FranceTv")
                        assertThat(it.cover).isEqualTo(FindCoverInformation(
                                url = URI("https://foo.bar.com"),
                                height = 123,
                                width = 456
                        ))
                        assertThat(it.description).isEqualTo("""Secrets d'Histoire est une émission de télévision présentée par Stéphane Bern. Chaque numéro retrace la vie d'un grand personnage de l'histoire et met en lumière des lieux hautement emblématiques du patrimoine. Magazine Secrets d'Histoire Accessible à tous, le magazine Secrets d’Histoire vous entraîne au cœur des épisodes mystérieux de l’histoire à travers des reportages, des enquêtes, des quizz… et bien plus encore ! En savoir plus""")
                    }
                    .verifyComplete()
        }

        @Test
        fun `information about france tv podcast with its url with no cover`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.france.tv/france-2/secrets-d-histoire/"

            whenever(imageService.fetchCoverInformation(any())).thenReturn(Mono.empty())
            backend.stubFor(get("/france-2/secrets-d-histoire/")
                    .willReturn(ok(IOUtils.fileAsString("/remote/podcast/francetv/secrets-d-histoire.home.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(url))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.url).isEqualTo(URI(url))
                        assertThat(it.title).isEqualTo("Secrets d'Histoire")
                        assertThat(it.type).isEqualTo("FranceTv")
                        assertThat(it.cover).isNull()
                        assertThat(it.description).isEqualTo("""Secrets d'Histoire est une émission de télévision présentée par Stéphane Bern. Chaque numéro retrace la vie d'un grand personnage de l'histoire et met en lumière des lieux hautement emblématiques du patrimoine. Magazine Secrets d'Histoire Accessible à tous, le magazine Secrets d’Histoire vous entraîne au cœur des épisodes mystérieux de l’histoire à travers des reportages, des enquêtes, des quizz… et bien plus encore ! En savoir plus""")
                    }
                    .verifyComplete()
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

    @Test
    fun `should do nothing on old implementation`() {
        /* Given */
        /* When */
        assertThatThrownBy { finder.find("") }
                /* Then */
                .hasMessage("An operation is not implemented: not required anymore")
    }

    @TestConfiguration
    @Import(FranceTvFinderConfig::class)
    class LocalTestConfiguration {

        @Bean fun imageService() = mock<ImageService>()
        @Bean fun webClientBuilder() = WebClient.builder()
                .filter(remapToMockServer("www.france.tv"))
    }
}
