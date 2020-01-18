package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.*
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
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
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService


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
        fun `with success with some cover null`(backend: WireMockServer) {
            /* Given */
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
        fun `with success with some publication date null`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            StepVerifier.create(updater.findItems(podcast).filter { it.pubDate == null })
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
                    .assertNext { assertThat(it).isEqualTo("9fd84a178f4b4d93384e4affb55b7d10") }
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


    @Nested
    @DisplayName("blocking")
    inner class Blocking {

        @Test
        fun `should not serve items with blocking method`() {
            /* Given */
            /* When */
            Assertions.assertThatThrownBy { updater.blockingFindItems(podcast) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }

        @Test
        fun `should not sign podcast with blocking method`() {
            /* Given */
            /* When */
            Assertions.assertThatThrownBy { updater.blockingSignatureOf(podcast.url) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }
    }

    @TestConfiguration
    @Import(RSSUpdaterConfig::class, WebClientAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun webClientCustomization() = WebClientCustomizer { wcb -> wcb.baseUrl("http://localhost:5555/") }
    }
}
