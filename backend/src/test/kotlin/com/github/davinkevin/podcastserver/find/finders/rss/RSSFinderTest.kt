package com.github.davinkevin.podcastserver.find.finders.rss

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
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
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

@ExtendWith(SpringExtension::class)
class RSSFinderTest(
        @Autowired val finder: RSSFinder
) {

    @MockitoBean lateinit var image: ImageService

    @Nested
    @DisplayName("should find")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        private val podcastUrl = "http://localhost:5555/rss.xml"
        private val coverUrl = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg"

        @Test
        fun `information about an rss podcast with its url`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.xml")))))
            whenever(image.fetchCoverInformation(URI(coverUrl)))
                .thenReturn(CoverInformation(100, 100, URI(coverUrl)))

            /* When */
            val podcast = finder.findPodcastInformation(podcastUrl)!!

            /* Then */
            assertAll {
                assertThat(podcast.title).isEqualToIgnoringCase("Les Grandes Gueules du Sport")
                assertThat(podcast.description).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ")
                assertThat(podcast.cover).isNotNull
                assertThat(podcast.cover?.url).isEqualTo(URI(coverUrl))
            }
        }

        @Test
        fun `information about an rss podcast with its url following redirections`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/rss.xml")
                        .willReturn(permanentRedirect("http://localhost:5555/rss-after-redirect.xml")))

                stubFor(get("/rss-after-redirect.xml")
                        .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.xml")))))
            }
            whenever(image.fetchCoverInformation(URI(coverUrl))).thenReturn(CoverInformation(100, 100, URI(coverUrl)))

            /* When */
            val podcast = finder.findPodcastInformation(podcastUrl)!!

            /* Then */
            assertAll {
                assertThat(podcast.title).isEqualToIgnoringCase("Les Grandes Gueules du Sport")
                assertThat(podcast.description).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ")
                assertThat(podcast.cover).isNotNull
                assertThat(podcast.cover!!.url).isEqualTo(URI(coverUrl))
            }
        }


        @Test
        fun `information with itunes cover`(backend: WireMockServer) {
            /* Given */
            whenever(image.fetchCoverInformation(URI(coverUrl))).thenReturn(CoverInformation(100, 100, URI(coverUrl)))
            backend.stubFor(get("/rss.xml")
                    .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.withItunesCover.xml")))))

            /* When */
            val podcast = finder.findPodcastInformation(podcastUrl)!!

            /* Then */
            assertAll {
                assertThat(podcast.cover).isNotNull
                assertThat(podcast.cover!!.url).isEqualTo(URI(coverUrl))
            }
        }

        @Test
        fun `information without any cover`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/rss.xml")
                    .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.withoutAnyCover.xml")))))

            /* When */
            val podcast = finder.findPodcastInformation(podcastUrl)!!

            /* Then */
            assertThat(podcast.cover).isNull()
        }

        @Test
        fun `and reject if not found`() {
            assertThatThrownBy {
                finder.findPodcastInformation("http://localhost:3578")!!
            }
        }
    }

    @Nested
    @DisplayName("should be compatible")
    inner class ShouldBeCompatible {

        @ParameterizedTest
        @ValueSource(strings = [
            "http://localhost:3578/foo/bar.rss",
            "http://another.format/foo/bar.rss",
            "http://no.one.else/a.rss"
        ])
        fun `with every url format`(url: String) {
            /* Given */
            /* When */
            val compatibility = finder.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1)
        }
    }

    companion object {
        fun from(s: String) = "/remote/podcast/rss/$s"
    }

    @TestConfiguration
    @Import(
        RSSFinderConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration
}
