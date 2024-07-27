package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime.now
import java.util.*

@ExtendWith(SpringExtension::class)
class RSSUpdaterTest(
        @Autowired val updater: RSSUpdater
){

    @TestConfiguration
    @Import(RSSUpdaterConfig::class, RestClientAutoConfiguration::class)
    class LocalTestConfiguration {
        @Bean fun webClientCustomization() = WebClientCustomizer { wcb -> wcb.baseUrl("http://localhost:5555/") }
    }

    @MockBean lateinit var image: ImageService

    private val podcast = PodcastToUpdate(UUID.randomUUID(), URI("http://localhost:5555/rss.xml"), "noSign")

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        @BeforeEach
        fun beforeEach() {
            whenever(image.fetchCoverInformation(any())).then { CoverInformation(100, 100, it.getArgument(0)) }
        }

        @Test
        fun `with success`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(217)
        }

        @Test
        fun `with fallback cover to rss image`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.with-only-rss-cover.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.cover?.url == URI("http://app-load.com/audio/rss/appload1400.jpg") }

            /* Then */
            assertThat(items).hasSize(215)
        }

        @Test
        fun `with fallback cover to itunes image`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.with-only-itunes-cover.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.cover?.url == URI("http://app-load.com/audio/itunes/appload1400.jpg") }

            /* Then */
            assertThat(items).hasSize(215)
        }

        @Test
        fun `with success without any cover`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.without-any-cover.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.cover == null }

            /* Then */
            assertThat(items).hasSize(215)
        }

        @Test
        fun `with success with podcast cover in error`(backend: WireMockServer) {
            /* Given */
            whenever(image.fetchCoverInformation(URI("http://app-load.com/audio/appload140.jpg")))
                .thenReturn(null)
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.cover == null }

            /* Then */
            assertThat(items).hasSize(215)
        }

        @Test
        fun `with success with item cover and podcast cover in error`(backend: WireMockServer) {
            /* Given */
            whenever(image.fetchCoverInformation(URI("http://app-load.com/audio/appload1400.jpg")))
                .thenReturn(null)
            whenever(image.fetchCoverInformation(URI("http://app-load.com/audio/appload140.jpg")))
                .thenReturn(null)
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.cover == null }

            /* Then */
            assertThat(items).hasSize(217)
        }

        @Test
        fun `with success with some publication date null`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { Duration.between(it.pubDate, now()).abs().seconds < 50 }

            /* Then */
            assertThat(items).hasSize(2)
        }

        @Test
        fun `with success with some publication with offset of 6 hours`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.pubDate?.offset == ZoneOffset.ofHours(6) }

            /* Then */
            assertThat(items).hasSize(1)
        }

        @Test
        fun `with success with some publication with offset of 8 hours`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.pubDate?.offset == ZoneOffset.ofHours(8) }

            /* Then */
            assertThat(items).hasSize(2)
        }

        @Test
        fun `with success with some publication with offset of 9 hours`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.pubDate?.offset == ZoneOffset.ofHours(9) }

            /* Then */
            assertThat(items).hasSize(1)
        }

        @Test
        fun `with success but flux empty because not channel in rss feed`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(""" <rss version="2.0"></rss>""")))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.cover?.url == URI("http://app-load.com/audio/rss/appload1400.jpg") }

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with success if xml link returns an empty page`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(ok()))

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `should support url with space in it by replacing it with + char`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.url.toASCIIString().contains("+") }

            /* Then */
            assertThat(items).hasSize(1)
        }

        @Test
        fun `should support enclosure without type`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(fileAsString("/remote/podcast/rss/rss.appload.xml"))))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.mimeType.contains("unknown") }

            /* Then */
            assertThat(items).hasSize(1)
        }

        @Test
        fun `should filter item without enclosure`(backend: WireMockServer) {
            /* Given */
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>AppLoad</title>
                        <link>http://www.app-load.com/</link>
                        <item>
                            <title>AppLoad 215 - N'invitez pas Cédric</title>
                            <link>http://feedproxy.google.com/~r/Appload/~3/zHhCKm4_NZs/</link>
                            <pubDate>Thu, 118 Jun 2015 14:30:25 +0200</pubDate> <!-- volontary wrong date  -->                            
                            <guid isPermaLink="false">63B5D695-F6CC-4704-B927-99B1862D82BF</guid>
                        </item>
                    </channel>
                </rss>
            """.trimIndent()
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(xml)))

            /* When */
            val items = updater.findItems(podcast)
                .filter { it.mimeType.contains("unknown") }

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `should support item without enclosure's length`(backend: WireMockServer) {
            /* Given */
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>AppLoad</title>
                        <link>http://www.app-load.com/</link>
                        <item>
                            <title>AppLoad 215 - N'invitez pas Cédric</title>
                            <link>http://feedproxy.google.com/~r/Appload/~3/zHhCKm4_NZs/</link>
                            <pubDate>Thu, 118 Jun 2015 14:30:25 +0200</pubDate> <!-- volontary wrong date  -->                            
                            <guid isPermaLink="false">63B5D695-F6CC-4704-B927-99B1862D82BF</guid>
                            <enclosure url='https://localhost:5555/item.mp3'/>
                        </item>
                    </channel>
                </rss>
            """.trimIndent()
            backend.stubFor(get("/rss.xml")
                    .willReturn(okTextXml(xml)))

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(1)
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
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("32909f0e501be25905b804be7c8360cc")
        }

        @Test
        fun `but return empty string if error`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(notFound()))

            /* When */
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("")
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
}
