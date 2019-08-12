package com.github.davinkevin.podcastserver.manager.worker.rss

import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.IOUtils.fileAsString
import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.service.CoverInformation
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okXml
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.test.StepVerifier
import java.net.URI

@Extensions(value = [ExtendWith(SpringExtension::class), ExtendWith(MockServer::class)])
@Import(RSSFinder::class)
@AutoConfigureWebClient
class RSSFinderTest {

    @Autowired lateinit var imageService: ImageService
    @Autowired lateinit var rssFinder: RSSFinder

    private val podcastUrl = "http://localhost:5555/rss.xml"

    @Test
    fun `should find information about an rss podcast with his url`(backend: WireMockServer) {
        /* Given */
        backend.stubFor(get("/rss.xml")
                .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.xml")))))
        whenever(imageService.fetchCoverInformation(COVER_URL)).thenReturn(CoverInformation(100, 100, URI(COVER_URL)))

        /* When */
        StepVerifier.create(rssFinder.findInformation(podcastUrl))
                /* Then */
                .expectSubscription()
                .assertNext { podcast ->
                    assertThat(podcast.title).isEqualToIgnoringCase("Les Grandes Gueules du Sport")
                    assertThat(podcast.description).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ")
                    assertThat(podcast.cover).isNotNull
                    assertThat(podcast.cover!!.url).isEqualTo(URI(COVER_URL))
                }
                .verifyComplete()
    }

    @Test
    fun `should find information with itunes cover`(backend: WireMockServer) {
        // Given
        whenever(imageService.fetchCoverInformation(COVER_URL)).thenReturn(CoverInformation(100, 100, URI(COVER_URL)))
        backend.stubFor(get("/rss.xml")
                .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.withItunesCover.xml")))))

        //When
        StepVerifier.create(rssFinder.findInformation(podcastUrl))
                /* Then */
                .expectSubscription()
                .assertNext { podcast ->
                    assertThat(podcast.cover).isNotNull
                    assertThat(podcast.cover!!.url).isEqualTo(URI(COVER_URL))
                }
                .verifyComplete()
    }

    @Test
    fun `should find information without any cover`(backend: WireMockServer) {
        /* Given */
        backend.stubFor(get("/rss.xml")
                .willReturn(okXml(fileAsString(from("rss.lesGrandesGueules.withoutAnyCover.xml")))))

        /* When */
        StepVerifier.create(rssFinder.findInformation(podcastUrl))
                /* Then */
                .expectSubscription()
                .assertNext { podcast ->
                    assertThat(podcast.cover).isNull()
                }
                .verifyComplete()
    }

    @Test
    fun `should reject if not found`() {
        /* Given */
        /* When */
        StepVerifier.create(rssFinder.findInformation("http://localhost:3578"))
                /* Then */
                .expectSubscription()
                .assertNext { podcast ->
                    assertThat(podcast).isEqualTo(FindPodcastInformation(title = "", url = URI("http://localhost:3578"), type = "RSS", cover = null, description = ""))
                }
                .verifyComplete()
    }

    companion object {
        private const val COVER_URL = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg"
        fun from(s: String) = "/remote/podcast/rss/$s"
    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun imageService() = mock<ImageService>()
    }
}
