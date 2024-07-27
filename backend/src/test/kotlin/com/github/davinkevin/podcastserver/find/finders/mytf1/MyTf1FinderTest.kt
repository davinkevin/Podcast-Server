package com.github.davinkevin.podcastserver.find.finders.mytf1

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
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
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

/**
 * Created by kevin on 12/01/2020
 */
@ExtendWith(SpringExtension::class)
class MyTf1FinderTest(
        @Autowired private val finder: MyTf1Finder
) {

    @MockBean lateinit var image: ImageService

    @TestConfiguration
    @Import(
        MyTf1FinderConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration {
        @Bean fun remapTf1Fr() = remapRestClientToMockServer("www.tf1.fr")
    }

    @Nested
    @ExtendWith(MockServer::class)
    @DisplayName("should find")
    inner class ShouldFind {

        @Test
        fun `information about tf1 podcast with its url`(backend: WireMockServer) {
            /* Given */
            val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"
            val coverUrl = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")

            whenever(image.fetchCoverInformation(coverUrl)).thenReturn(CoverInformation(
                    url = coverUrl,
                    height = 123,
                    width = 456
            ))

            backend.stubFor(get("/tmc/quotidien-avec-yann-barthes")
                .willReturn(ok(fileAsString("/remote/podcast/mytf1/quotidien.root.html"))))

            /* When */
            val podcast = finder.findPodcastInformation(url)!!

            /* Then */
            assertThat(podcast).isEqualTo(FindPodcastInformation(
                title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                description = "Yann Barthès est désormais sur TMC et TF1",
                url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                cover = FindCoverInformation(
                    height = 123,
                    width = 456,
                    url = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")
                ),
                type= "MyTF1"
            ))
        }

        @Nested
        @DisplayName("information with")
        inner class InformationWithCover {

            @Test
            fun `no cover url`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"

                backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                        ok(fileAsString("/remote/podcast/mytf1/quotidien.root-without-picture.html"))
                ))

                /* When */
                val podcast = finder.findPodcastInformation(url)!!

                /* Then */
                assertThat(podcast).isEqualTo(FindPodcastInformation(
                    title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                    description = "Yann Barthès est désormais sur TMC et TF1",
                    url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                    cover = null,
                    type= "MyTF1"
                ))
            }

            @Test
            fun `empty url for cover`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"

                backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                        ok(fileAsString("/remote/podcast/mytf1/quotidien.root-without-picture-url.html"))
                ))

                /* When */
                val podcast = finder.findPodcastInformation(url)!!

                /* Then */
                assertThat(podcast).isEqualTo(FindPodcastInformation(
                    title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                    description = "Yann Barthès est désormais sur TMC et TF1",
                    url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                    cover = null,
                    type= "MyTF1"
                ))
            }

            @Test
            fun `relative url for cover`(backend: WireMockServer) {
                /* Given */
                val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"
                val coverUrl = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")

                whenever(image.fetchCoverInformation(coverUrl)).thenReturn(CoverInformation(
                        url = coverUrl,
                        height = 123,
                        width = 456
                ))

                backend.stubFor(get("/tmc/quotidien-avec-yann-barthes").willReturn(
                        ok(fileAsString("/remote/podcast/mytf1/quotidien.root-with-picture-url-relative.html"))
                ))

                /* When */
                val podcast = finder.findPodcastInformation(url)!!

                /* Then */
                assertThat(podcast).isEqualTo(FindPodcastInformation(
                    title = "Quotidien avec Yann Barthès - TMC | MYTF1",
                    description = "Yann Barthès est désormais sur TMC et TF1",
                    url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"),
                    cover = FindCoverInformation(
                        height = 123,
                        width = 456,
                        url = URI("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")
                    ),
                    type= "MyTF1"
                ))
            }

        }
    }

    @Nested
    @DisplayName("on compatibility")
    inner class OnCompatibility {

        @ParameterizedTest(name = "with {0}")
        @ValueSource(strings = [
            "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes",
            "https://www.tf1.fr/tf1/tout-est-permis-avec-arthur",
            "https://www.tf1.fr/tf1/les-douze-coups-de-midi",
            "https://www.tf1.fr/tf1/greys-anatomy"
        ])
        fun `should be compatible `(/* Given */ url: String) {
            /* When */
            val compatibility = finder.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @ParameterizedTest(name = "with {0}")
        @ValueSource(strings = [
            "https://www.france2.tv/france-2/vu/",
            "https://www.foo.com/france-2/vu/",
            "https://www.mycanal.fr/france-2/vu/",
            "https://www.gulli.fr/france-2/vu/"
        ])
        fun `should not be compatible`(/* Given */ url: String) {
            /* When */
            val compatibility = finder.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
        }
    }

}
