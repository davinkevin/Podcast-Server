package com.github.davinkevin.podcastserver.find.finders.itunes

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinder
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI

/**
 * Created by kevin on 12/05/2018
 */
@ExtendWith(SpringExtension::class, MockServer::class)
@AutoConfigureJson
class ItunesFinderTest(
    @Autowired val finder: ItunesFinder
) {

    @MockBean lateinit var rssFinder: RSSFinder

    @Test
    fun `should be compatible with "podcasts dot apple dot com" url`() {
        /* GIVEN */
        val url = "https://itunes.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2"
        /* WHEN  */
        val compatibilityLevel = finder.compatibility(url)
        /* THEN  */
        assertThat(compatibilityLevel).isEqualTo(1)
    }

    @Test
    fun `should be compatible with "itunes dot apple dot com" url`() {
        /* GIVEN */
        val url = "https://podcasts.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2"
        /* WHEN  */
        val compatibilityLevel = finder.compatibility(url)
        /* THEN  */
        assertThat(compatibilityLevel).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* GIVEN */
        val url = "https://foo.bar.com/fr/podcast/foo/idbar"
        /* WHEN  */
        val compatibilityLevel = finder.compatibility(url)
        /* THEN  */
        assertThat(compatibilityLevel).isGreaterThan(1)
    }

    @Test
    fun `should find url`(backend: WireMockServer) {
        /* GIVEN */
        val url = "https://podcasts.apple.com/fr/podcast/positron/id662892474"
        backend.stubFor(get("/lookup?id=662892474").willReturn(okJson(fileAsString("/remote/podcast/itunes/lookup.json"))))
        val podcastInformation = FindPodcastInformation(
                title = "Positron",
                description = "Zapping de bons plans !<br /> <br /> Positron, c'est une émissions dynamique qui vous promet de « ne plus jamais vous ennuyer » en vous recommandant des livres, films, séries ou albums que vous ne connaissez peut-être pas encore, mais que vous adorerez bientôt !",
                url = URI("http://feeds.feedburner.com/emissionpositron"),
                cover = FindCoverInformation(height = 140, width = 140, url = URI("http://frenchspin.com/sites/positron/audio/positron140.png") ),
                type = "RSS"
        )
        whenever(rssFinder.findInformation("http://feeds.feedburner.com/emissionpositron"))
                .thenReturn(podcastInformation.toMono())

        /* WHEN  */
        StepVerifier.create(finder.findInformation(url))
                /* THEN  */
                .expectSubscription()
                .assertNext { assertThat(it).isSameAs(podcastInformation) }
                .verifyComplete()
    }

    @TestConfiguration
    @Import(ItunesFinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun webClientCustomization() = WebClientCustomizer { it.filter(remapToMockServer("podcasts.apple.com")) }
    }
}
