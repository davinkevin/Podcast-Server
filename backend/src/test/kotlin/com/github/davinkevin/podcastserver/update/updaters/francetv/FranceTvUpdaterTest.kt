package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.DigestUtils
import reactor.core.publisher.Mono
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

    @TestConfiguration
    @Import(FranceTvUpdaterConfig::class, WebClientAutoConfiguration::class, WebClientConfig::class, JacksonAutoConfiguration::class)
    class LocalTestConfiguration {
        @Bean fun remapFranceTvToMock() = remapToMockServer("www.france.tv")
        @Bean fun remapApiToMock() = remapToMockServer("player.webservices.francetelevisions.fr")
        @Bean fun imageMockServer() = remapToMockServer("assets.webservices.francetelevisions.fr")
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

            if (coverPath.isEmpty()) return
            stubFor(get(coverPath).willReturn(aResponse().withBodyFile("img/image.png")))
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/no-item.html"))))
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with no downloadable item`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                /* Then */
                .expectSubscription()
                .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                /* Then */
                .expectSubscription()
                .assertNext {
                    assertThat(it.title).isEqualTo("Marie-Madeleine : si près de Jésus...")
                    assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2024, 3, 20, 21, 13, 0, 0, ZoneOffset.ofHours(1)))
                    assertThat(it.length).isEqualTo(6881)
                    assertThat(it.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html"))
                    assertThat(it.description).isEqualTo("Si la Bible regorge de personnages mystérieux et fascinants, Marie-Madeleine occupe une place à part parmi eux. Sa proximité avec Jésus éveille les passions et les polémiques les plus folles. Que représente Marie-Madeleine aux yeux et dans le cœur de Jésus ? Doit-on croire à un amour impossible, ou à des sentiments plus ardents ? Car il s'agit bien d'un \"mystère Marie-Madeleine\", qui tient tout d'abord aux différentes sources qui nous sont parvenues et qui ne s'accordent pas toujours sur la véritable identité, ni sur le rôle qu'a joué cette femme. Pour mieux la comprendre, Stéphane Bern foule la terre de Jérusalem, là où tout a commencé et ainsi tenter de déchiffrer les différents Évangiles.")
                    assertThat(it.cover!!.height).isEqualTo(300)
                    assertThat(it.cover!!.width).isEqualTo(256)
                    assertThat(it.cover!!.url).isEqualTo(URI("https://www.france.tv/image/carre/1024/1024/q/4/c/1ae0e33f-phpg7tc4q.jpg"))
                }
                .verifyComplete()
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV6(
                    path = "/france-3/secrets-d-histoire/saison-18/5766417-marie-madeleine-si-pres-de-jesus.html"
                )
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                /* Then */
                .expectSubscription()
                .assertNext {
                    assertThat(it.cover).isNull()
                }
                .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                /* Then */
                .expectSubscription()
                .expectNextCount(10)
                .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/no-item.html"))))
            }
            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                /* Then */
                .expectSubscription()
                .assertNext { assertThat(it).isEqualTo("") }
                .verifyComplete()
        }

        @Test
        fun `with no downloadable items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
            }
            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                /* Then */
                .expectSubscription()
                .assertNext { assertThat(it).isEqualTo("") }
                .verifyComplete()
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v6/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                /* Then */
                .expectSubscription()
                .assertNext { assertThat(it).isEqualTo("e40066f26d1ddb195fd89578b158abf3") }
                .verifyComplete()

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

            val dualSign = Mono.zip(updater.signatureOf(podcast.url), updater.signatureOf(podcast.url))

            /* When */
            StepVerifier.create(dualSign)
                /* Then */
                .expectSubscription()
                .assertNext { (first, second) ->
                    assertThat(first).isEqualTo("e40066f26d1ddb195fd89578b158abf3")
                    assertThat(second).isEqualTo("e40066f26d1ddb195fd89578b158abf3")
                }
                .verifyComplete()
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
