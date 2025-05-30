package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.DigestUtils
import java.net.URI
import java.time.*
import java.util.*

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

@ExtendWith(SpringExtension::class)
@AutoConfigureObservability
class FranceTvUpdaterTest(
    @Autowired private val updater: FranceTvUpdater
) {

    @MockitoBean lateinit var imageService: ImageService

    @TestConfiguration
    @Import(
        FranceTvUpdaterConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration {
        @Bean fun remapFranceTvToMock() = remapRestClientToMockServer("www.france.tv")
        @Bean fun remapApiToMock() = remapRestClientToMockServer("player.webservices.francetelevisions.fr")
        @Bean fun imageMockServer() = remapRestClientToMockServer("assets.webservices.francetelevisions.fr")
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

        private fun WireMockServer.forV7(path: String, coverPath: String = "") {
            stubFor(get(path)
                .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/videos/${path.substringAfterLast("/")}"))))

            if (coverPath.isEmpty()) {
                whenever(imageService.fetchCoverInformation(any<URI>())).thenReturn(null)
                return
            }
            val uri = URI("https://medias.france.tv$coverPath")
            whenever(imageService.fetchCoverInformation(uri)).thenReturn(CoverInformation(
                width = 256, height = 300, url = uri
            ))
        }

        @Test
        fun `with no content on all video page`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok()))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/no-item.html"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with no downloadable item`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with one downloadable item`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV7(
                    path = "/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html",
                    coverPath = "/_cW692thJEUi4VUnXHzmu8BoRO0/880x0/filters:quality(85):format(jpeg)/7/q/1/phpaa51q7.jpg"
                )
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertAll {
                assertThat(items).hasSize(1)
                val item = items.first()
                assertThat(item.title).isEqualTo("La folle Ã©popÃ©e de Charlotte d'Angleterre en replay - Secrets d'Histoire")
                assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(2025, 5, 21, 21, 7, 0, 0, ZoneId.of("Europe/Paris")))
                assertThat(item.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html"))
                assertThat(item.description).isEqualTo("StÃ©phane Bern raconte l'Ã©popÃ©e de Charlotte d'Angleterre, grand-mÃ¨re de la reine Victoria et aÃ¯eule de l'actuel roi Charles III. Cette souveraine est Ã  l'ori...")
                assertThat(item.cover!!.height).isEqualTo(300)
                assertThat(item.cover.width).isEqualTo(256)
                assertThat(item.cover.url).isEqualTo(URI("https://medias.france.tv/_cW692thJEUi4VUnXHzmu8BoRO0/880x0/filters:quality(85):format(jpeg)/7/q/1/phpaa51q7.jpg"))
            }
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV7(path = "/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html")
            }

            /* When */
            val items = updater.findItems(podcast)

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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/all.html"))))

                forV7("/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html")
                forV7("/france-3/secrets-d-histoire/saison-19/7043581-la-marquise-de-brinvilliers-et-l-affaire-des-poisons.html")
                forV7("/france-3/secrets-d-histoire/saison-19/6912070-les-amants-tragiques-de-mayerling.html")
                forV7("/france-3/secrets-d-histoire/saison-19/6851503-louis-xi-un-regne-de-terreur.html")
                forV7("/france-3/secrets-d-histoire/saison-18/6812617-pauline-borghese-la-diva-de-l-empire.html")
                forV7("/france-3/secrets-d-histoire/saison-18/6673895-robin-des-bois-le-prince-des-voleurs.html")
                forV7("/france-3/secrets-d-histoire/saison-18/6653774-spartacus-et-la-revolte-des-gladiateurs.html")
                forV7("/france-3/secrets-d-histoire/2801349-les-secrets-des-templiers.html")
                forV7("/france-3/secrets-d-histoire/660047-agatha-christie-l-etrange-reine-du-crime-bande-annonce.html")
                forV7("/france-3/secrets-d-histoire/222009-alienor-d-aquitaine-une-rebelle-au-moyen-age-bande-annonce.html")
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(10)
        }

        @Test
        fun `with item not found`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html")
                    .willReturn(ok()))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `and return an element without date because uploadDate is not defined `(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/videos/case/7153193-without-date.html"))))

                val uri = URI("https://medias.france.tv/_cW692thJEUi4VUnXHzmu8BoRO0/880x0/filters:quality(85):format(jpeg)/7/q/1/phpaa51q7.jpg")
                whenever(imageService.fetchCoverInformation(uri))
                    .thenReturn(CoverInformation(width = 256, height = 300, url = uri))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertAll {
                assertThat(items).hasSize(1)
                val item = items.first()
                assertThat(item.title).isEqualTo("La folle Ã©popÃ©e de Charlotte d'Angleterre en replay - Secrets d'Histoire")
                assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC))
                assertThat(item.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html"))
                assertThat(item.description).isEqualTo("StÃ©phane Bern raconte l'Ã©popÃ©e de Charlotte d'Angleterre, grand-mÃ¨re de la reine Victoria et aÃ¯eule de l'actuel roi Charles III. Cette souveraine est Ã  l'ori...")
                assertThat(item.cover!!.height).isEqualTo(300)
                assertThat(item.cover.width).isEqualTo(256)
                assertThat(item.cover.url).isEqualTo(URI("https://medias.france.tv/_cW692thJEUi4VUnXHzmu8BoRO0/880x0/filters:quality(85):format(jpeg)/7/q/1/phpaa51q7.jpg"))
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
            val signature = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/no-item.html"))))
            }

            /* When */
            val signature = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }

        @Test
        fun `with no downloadable items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
            }

            /* When */
            val signature = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            val signature = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("932e4c66269adf90931dfb870d3f6599")
            val hash = DigestUtils.md5DigestAsHex("/france-3/secrets-d-histoire/222009-alienor-d-aquitaine-une-rebelle-au-moyen-age-bande-annonce.html-/france-3/secrets-d-histoire/2801349-les-secrets-des-templiers.html-/france-3/secrets-d-histoire/660047-agatha-christie-l-etrange-reine-du-crime-bande-annonce.html-/france-3/secrets-d-histoire/saison-18/6653774-spartacus-et-la-revolte-des-gladiateurs.html-/france-3/secrets-d-histoire/saison-18/6673895-robin-des-bois-le-prince-des-voleurs.html-/france-3/secrets-d-histoire/saison-18/6812617-pauline-borghese-la-diva-de-l-empire.html-/france-3/secrets-d-histoire/saison-19/6851503-louis-xi-un-regne-de-terreur.html-/france-3/secrets-d-histoire/saison-19/6912070-les-amants-tragiques-de-mayerling.html-/france-3/secrets-d-histoire/saison-19/7043581-la-marquise-de-brinvilliers-et-l-affaire-des-poisons.html-/france-3/secrets-d-histoire/saison-19/7153193-la-folle-epopee-de-charlotte-d-angleterre.html".toByteArray())
            assertThat(hash).isEqualTo("932e4c66269adf90931dfb870d3f6599")
        }

        @Test
        fun `consistent between two executions`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v7/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            val first = updater.signatureOf(podcast.url)
            val second = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(first).isEqualTo("932e4c66269adf90931dfb870d3f6599")
            assertThat(second).isEqualTo("932e4c66269adf90931dfb870d3f6599")
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
