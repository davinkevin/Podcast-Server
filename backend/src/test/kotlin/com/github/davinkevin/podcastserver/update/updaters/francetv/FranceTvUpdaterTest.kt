package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.DigestUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.test.StepVerifier
import java.net.URI
import java.time.*
import java.util.*

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

@ExtendWith(SpringExtension::class)
class FranceTvUpdaterTest(
    @Autowired private val updater: FranceTvUpdater
) {

    @MockBean lateinit var imageService: ImageService

    @TestConfiguration
    @Import(
        FranceTvUpdaterConfig::class,
        RestClientAutoConfiguration::class,
        WebClientConfig::class,
        JacksonAutoConfiguration::class
    )
    class LocalTestConfiguration {
        @Bean fun remapFranceTvToMock() = remapRestClientToMockServer("www.france.tv")
        @Bean fun remapApiToMock() = remapRestClientToMockServer("player.webservices.francetelevisions.fr")
        @Bean fun imageMockServer() = remapRestClientToMockServer("assets.webservices.francetelevisions.fr")
        @Bean fun assetsOnFranceTv() = remapToMockServer("www.france.tv")
        @Bean fun fixedClock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
    }

    private val podcast = PodcastToUpdate(
        id = UUID.randomUUID(),
        url = URI("https://www.france.tv/france-3/secrets-d-histoire"),
        signature = "old_signature"
    )

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        private fun WireMockServer.forV6(path: String, coverPath: String = "") {
            stubFor(get(path)
                .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/videos/${path.substringAfterLast("/")}"))))

            if (coverPath.isEmpty()) {
                whenever(imageService.fetchCoverInformation(any<URI>())).thenReturn(Mono.empty())
                return
            }

            val uri = URI("https://www.france.tv$coverPath")
            whenever(imageService.fetchCoverInformation(uri)).thenReturn(CoverInformation(
                width = 256, height = 300, url = uri
            ).toMono())
        }

        @Test
        fun `with no content on all video page`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok()))
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/no-item.html"))))
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with no downloadable item`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with one downloadable item`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV6(
                    path = "/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html",
                    coverPath = "/image/carre/1024/1024/q/4/c/1ae0e33f-phpg7tc4q.jpg"
                )
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertAll {
                assertThat(items).hasSize(1)
                val item = items.first()
                assertThat(item.title).isEqualTo("Marie-Madeleine : si près de Jésus...")
                assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(2024, 3, 20, 21, 13, 0, 0, ZoneOffset.ofHours(1)))
                assertThat(item.length).isEqualTo(6881)
                assertThat(item.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html"))
                assertThat(item.description).isEqualTo("Si la Bible regorge de personnages mystérieux et fascinants, Marie-Madeleine occupe une place à part parmi eux. Sa proximité avec Jésus éveille les passions et les polémiques les plus folles. Que représente Marie-Madeleine aux yeux et dans le cœur de Jésus ? Doit-on croire à un amour impossible, ou à des sentiments plus ardents ? Car il s'agit bien d'un \"mystère Marie-Madeleine\", qui tient tout d'abord aux différentes sources qui nous sont parvenues et qui ne s'accordent pas toujours sur la véritable identité, ni sur le rôle qu'a joué cette femme. Pour mieux la comprendre, Stéphane Bern foule la terre de Jérusalem, là où tout a commencé et ainsi tenter de déchiffrer les différents Évangiles.")
                assertThat(item.cover!!.height).isEqualTo(300)
                assertThat(item.cover!!.width).isEqualTo(256)
                assertThat(item.cover!!.url).isEqualTo(URI("https://www.france.tv/image/carre/1024/1024/q/4/c/1ae0e33f-phpg7tc4q.jpg"))
            }
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV6("/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html")
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertAll {
                assertThat(items).hasSize(1)
                assertThat(items.first().cover).isNull()
            }
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all.html"))))

                forV6("/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html")
                forV6("/france-3/secrets-d-histoire/3007545-l-incroyable-epopee-de-richard-coeur-de-lion.html")
                forV6("/france-3/secrets-d-histoire/saison-18/5731266-philippe-v-les-demons-du-roi-d-espagne.html")
                forV6("/france-3/secrets-d-histoire/2772025-louis-xv-et-la-bete-du-gevaudan.html")
                forV6("/france-3/secrets-d-histoire/saison-18/5672001-au-danemark-le-roi-la-reine-et-le-seduisant-docteur.html")
                forV6("/france-3/secrets-d-histoire/2759591-philippe-le-bel-et-l-etrange-affaire-des-templiers.html")
                forV6("/france-3/secrets-d-histoire/saison-18/5587902-arthur-et-les-chevaliers-de-la-table-ronde.html")
                forV6("/france-3/secrets-d-histoire/5475558-vatel-careme-escoffier-a-la-table-des-rois.html")
                forV6("/france-3/secrets-d-histoire/secrets-d-histoire-saison-17/5403759-napoleon-iii-le-dernier-empereur-des-francais.html")
                forV6("/sport/les-jeux-olympiques/4211497-paris-2024-stephane-bern-livre-les-secrets-d-une-marche-historique.html")
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertThat(items).hasSize(10)
        }

        @Test
        fun `with item not found`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html")
                    .willReturn(ok()))
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `and return no element because no ld+json`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/videos/case/5766417.without-application-ld-json.html"))))
            }

            /* When */
            assertThatThrownBy { updater.findItemsBlocking(podcast) }
                /* Then */
                .hasMessage("No <script type=\"application/ld+json\"></script> found")
        }

        @Test
        fun `and return no element because no VideoObject `(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/videos/case/5766417.without-videoobject.html"))))
            }

            /* When */
            assertThatThrownBy { updater.findItemsBlocking(podcast) }
                /* Then */
                .hasMessage("No element of type VideoObject")
        }

        @Test
        fun `and return an element without date because uploadDate is not defined `(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/videos/case/5766417.without-pubdate.html"))))

                val uri = URI("https://www.france.tv/image/carre/1024/1024/q/4/c/1ae0e33f-phpg7tc4q.jpg")
                whenever(imageService.fetchCoverInformation(uri))
                    .thenReturn(CoverInformation(width = 256, height = 300, url = uri).toMono())
            }

            /* When */
            val items = updater.findItemsBlocking(podcast)

            /* Then */
            assertAll {
                assertThat(items).hasSize(1)
                val item = items.first()
                assertThat(item.title).isEqualTo("Marie-Madeleine : si près de Jésus...")
                assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC))
                assertThat(item.length).isEqualTo(6881)
                assertThat(item.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html"))
                assertThat(item.description).isEqualTo("Si la Bible regorge de personnages mystérieux et fascinants, Marie-Madeleine occupe une place à part parmi eux. Sa proximité avec Jésus éveille les passions et les polémiques les plus folles. Que représente Marie-Madeleine aux yeux et dans le cœur de Jésus ? Doit-on croire à un amour impossible, ou à des sentiments plus ardents ? Car il s'agit bien d'un \"mystère Marie-Madeleine\", qui tient tout d'abord aux différentes sources qui nous sont parvenues et qui ne s'accordent pas toujours sur la véritable identité, ni sur le rôle qu'a joué cette femme. Pour mieux la comprendre, Stéphane Bern foule la terre de Jérusalem, là où tout a commencé et ainsi tenter de déchiffrer les différents Évangiles.")
                assertThat(item.cover!!.height).isEqualTo(300)
                assertThat(item.cover!!.width).isEqualTo(256)
                assertThat(item.cover!!.url).isEqualTo(URI("https://www.france.tv/image/carre/1024/1024/q/4/c/1ae0e33f-phpg7tc4q.jpg"))
            }
        }
    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with empty response to the page request`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok()))
            }

            /* When */
            val signature = updater.signatureOfBlocking(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/no-item.html"))))
            }

            /* When */
            val signature = updater.signatureOfBlocking(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }

        @Test
        fun `with no downloadable items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
            }

            /* When */
            val signature = updater.signatureOfBlocking(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            val signature = updater.signatureOfBlocking(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("e40066f26d1ddb195fd89578b158abf3")
            val hash = DigestUtils.md5DigestAsHex("/france-3/secrets-d-histoire/2759591-philippe-le-bel-et-l-etrange-affaire-des-templiers.html-/france-3/secrets-d-histoire/2772025-louis-xv-et-la-bete-du-gevaudan.html-/france-3/secrets-d-histoire/3007545-l-incroyable-epopee-de-richard-coeur-de-lion.html-/france-3/secrets-d-histoire/5475558-vatel-careme-escoffier-a-la-table-des-rois.html-/france-3/secrets-d-histoire/saison-18/5587902-arthur-et-les-chevaliers-de-la-table-ronde.html-/france-3/secrets-d-histoire/saison-18/5672001-au-danemark-le-roi-la-reine-et-le-seduisant-docteur.html-/france-3/secrets-d-histoire/saison-18/5731266-philippe-v-les-demons-du-roi-d-espagne.html-/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html-/france-3/secrets-d-histoire/secrets-d-histoire-saison-17/5403759-napoleon-iii-le-dernier-empereur-des-francais.html-/sport/les-jeux-olympiques/4211497-paris-2024-stephane-bern-livre-les-secrets-d-une-marche-historique.html".toByteArray())
            assertThat(hash).isEqualTo("e40066f26d1ddb195fd89578b158abf3")
        }

        @Test
        fun `consistent between two executions`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            val first = updater.signatureOfBlocking(podcast.url)
            val second = updater.signatureOfBlocking(podcast.url)

            /* Then */
            assertThat(first).isEqualTo("e40066f26d1ddb195fd89578b158abf3")
            assertThat(second).isEqualTo("e40066f26d1ddb195fd89578b158abf3")
        }
    }

    @Test
    fun `should return franceTv type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("FranceTv")
        assertThat(type.name).isEqualTo("France•tv")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible`() {
            /* Given */
            val url = "https://www.france.tv/france-3/secrets-d-histoire"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
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
