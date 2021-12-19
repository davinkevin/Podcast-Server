package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.*
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.time.ZoneOffset
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.ZonedDateTime.now


/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@ExtendWith(SpringExtension::class)
class RSSUpdaterTest(
        @Autowired val updater: RSSUpdater
){

    @MockBean lateinit var image: ImageService

    private val podcast = PodcastToUpdate(UUID.randomUUID(), URI("http://localhost:5555/rss.xml"), "noSign")

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        @BeforeEach
        fun beforeEach() {
            whenever(image.fetchCoverInformation(any())).then { CoverInformation(100, 100, it.getArgument(0)).toMono() }
        }

        @Test
        fun `with success`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(217)
                    .verifyComplete()
        }

        @Test
        fun `with fallback cover to rss image`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.with-only-rss-cover.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast)
                    .filter { it.cover?.url == URI("http://app-load.com/audio/rss/appload1400.jpg") }
            )
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(215)
                    .verifyComplete()
        }

        @Test
        fun `with fallback cover to itunes image`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.with-only-itunes-cover.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast)
                    .filter { it.cover?.url == URI("http://app-load.com/audio/itunes/appload1400.jpg") }
            )
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(215)
                    .verifyComplete()
        }

        @Test
        fun `with success without any cover`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.without-any-cover.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.cover == null })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(215)
                    .verifyComplete()
        }

        @Test
        fun `with success with podcast cover in error`(backend: WireMockServer) {
            /* Given */
            whenever(image.fetchCoverInformation(URI("http://app-load.com/audio/appload140.jpg")))
                    .then { Mono.empty<CoverInformation>() }
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.cover == null })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(215)
                    .verifyComplete()
        }

        @Test
        fun `with success with item cover and podcast cover in error`(backend: WireMockServer) {
            /* Given */
            whenever(image.fetchCoverInformation(URI("http://app-load.com/audio/appload1400.jpg")))
                    .then { Mono.empty<CoverInformation>() }
            whenever(image.fetchCoverInformation(URI("http://app-load.com/audio/appload140.jpg")))
                    .then { Mono.empty<CoverInformation>() }
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.cover == null })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(217)
                    .verifyComplete()
        }

        @Test
        fun `with success with some publication date null`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast)
                    .filter { Duration.between(it.pubDate, now()).abs().seconds < 50 }
            )
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(1)
                    .verifyComplete()
        }

        @Test
        fun `with success with some publication with offset of 6 hours`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.pubDate?.offset == ZoneOffset.ofHours(6) })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(1)
                    .verifyComplete()
        }

        @Test
        fun `with success with some publication with offset of 8 hours`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.pubDate?.offset == ZoneOffset.ofHours(8) })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(2)
                    .verifyComplete()
        }

        @Test
        fun `with success with some publication with offset of 9 hours`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.pubDate?.offset == ZoneOffset.ofHours(9) })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(1)
                    .verifyComplete()
        }


        @Test
        fun `with success but flux empty because not channel in rss feed`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(""" <rss version="2.0"></rss>""")))

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `should support url with space in it by replacing it with + char`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.url.toASCIIString().contains("+") })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(1)
                    .verifyComplete()
        }

        @Test
        fun `should support enclosure without type`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.mimeType.contains("unknown") })
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(1)
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with success`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isEqualTo("baf07ee03f27ddb7ea8114d766021993") }
                    .verifyComplete()
        }

        @Test
        fun `but return empty string if error`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(notFound()))

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isEqualTo("error_during_update") }
                    .verifyComplete()
        }

    }

    @Test
    fun `should return rss type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("RSS")
        assertThat(type.name).isEqualTo("RSS")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible`() {
            /* Given */
            val url = "https://foo.bar.com"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1)
        }

        @Test
        fun `should not be compatible`() {
            /* Given */
            val url = "grpc://foo.bar.com"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }

    @TestConfiguration
    @Import(RSSUpdaterConfig::class, WebClientAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun webClientCustomization() = WebClientCustomizer { wcb -> wcb.baseUrl("http://localhost:5555/") }
    }
}
