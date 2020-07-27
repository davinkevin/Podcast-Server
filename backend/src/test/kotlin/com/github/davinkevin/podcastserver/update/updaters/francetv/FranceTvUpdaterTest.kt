package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@ExtendWith(SpringExtension::class)
class FranceTvUpdaterTest(
        @Autowired private val updater: FranceTvUpdater
) {

    @TestConfiguration
    @Import(FranceTvUpdaterConfig::class, WebClientAutoConfiguration::class, WebClientConfig::class, JacksonAutoConfiguration::class)
    class LocalTestConfiguration {
        @Bean fun remapFranceTvToMock() = remapToMockServer("www.france.tv")
        @Bean fun remapApiToMock() = remapToMockServer("sivideo.webservices.francetelevisions.fr")
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

        private fun WireMockServer.forV3(name: String, id: String = "", coverPath: String = "") {
            stubFor(get("/france-3/secrets-d-histoire/$name.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/items/$name.html"))))

            if(id.isEmpty()) return
            stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=$id")
                    .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/$id.json"))))

            if (coverPath.isEmpty()) return
            stubFor(get(coverPath).willReturn(aResponse().withBodyFile("img/image.png")))
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                        .willReturn(okTextXml(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-no-items.html"))))
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with no downloadable items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                        .willReturn(okTextXml(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-no-downloadable-items.html"))))
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
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-one-item.html"))))
                forV3("1289373-madame-de-montespan-le-grand-amour-du-roi-soleil", "2d8cde13-4b9d-423f-95c8-b37151b989bc", "/staticftv/ref_emissions/2020-03-09/EMI_954457.jpg")
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Secrets d'histoire - Madame de Montespan, le grand amour du Roi Soleil")
                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2020, 3, 9, 21, 5, 0, 0, ZoneId.of("Europe/Paris")))
                        assertThat(it.length).isNull()
                        assertThat(it.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html"))
                        assertThat(it.description).isEqualTo("La Marquise de Montespan a régné pendant plus de dix ans sur le coeur du Roi Soleil, Louis XIV. Qui était-elle ? Françoise de Rochechouart de Mortemart est issue de l'une des plus anciennes souches aristocratiques du Royaume. A Paris, elle épouse Louis-Henri de Pardaillan de Gondrin, marquis de Montespan. Au printemps 1663, elle devient demoiselle d'honneur de la Reine, Marie-Thérèse qui se désole déjà des frasques de son auguste mari. Louis XIV est immédiatement séduit. Mais le mari de la Montespan demande réparation. Trop bruyant : on décide de l'enfermer. Il doit s'exiler. La favorite insuffle alors à Versailles un esprit de liberté : elle encourage Racine ; Molière lui dédit «L'Ecole des femmes» ; elle protège La Fontaine.")
                        assertThat(it.cover!!.height).isEqualTo(300)
                        assertThat(it.cover!!.width).isEqualTo(256)
                        assertThat(it.cover!!.url).isEqualTo(URI("https://sivideo.webservices.francetelevisions.fr/staticftv/ref_emissions/2020-03-09/EMI_954457.jpg"))
                    }
                    .verifyComplete()
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/items/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html"))))
                 stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                    .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/2d8cde13-4b9d-423f-95c8-b37151b989bc.json"))))
                stubFor(get("/staticftv/ref_emissions/2020-03-09/EMI_954457.jpg")
                    .willReturn(notFound()))
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
        fun `with one item totally null`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/items/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/item-null.json"))))
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.html"))))

                forV3("1146545-splendeur-et-decheance-de-lady-hamilton", "22d058da-a2e6-4af8-87cd-142f0c49976d", "/staticftv/ref_emissions/2020-01-20/EMI_945754.jpg")
                forV3("1213409-le-prince-philip-au-service-de-sa-majeste", "7723a7d2-7c0d-4f31-a7eb-ac753b8ec225", "/staticftv/ref_emissions/2020-02-10/EMI_946874.jpg")
                forV3("1241451-le-prince-imperial-ou-la-fureur-de-vivre", "8c005cf9-fc29-4e29-a3e6-af2fde69a283", "/staticftv/ref_emissions/2020-02-17/EMI_949810.jpg")
                forV3("1289373-madame-de-montespan-le-grand-amour-du-roi-soleil", "2d8cde13-4b9d-423f-95c8-b37151b989bc", "/staticftv/ref_emissions/2020-03-09/EMI_954457.jpg")
            }

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(4)
                    .verifyComplete()
        }

        @Nested
        @DisplayName("with specific characteristics")
        inner class WithSpecificCharacteristics {

            @BeforeEach
            fun beforeEach(backend: WireMockServer) {
                backend.apply {
                    stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/").willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-one-item.html"))))

                    stubFor(get("/france-3/secrets-d-histoire/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html")
                            .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/items/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html"))))
                    stubFor(get("/staticftv/ref_emissions/2020-03-09/EMI_954457.jpg").willReturn(aResponse().withBodyFile("img/image.png")))
                }
            }

            @Test
            fun `with season, episode and subtitle fields`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific/2d8cde13-4b9d-423f-95c8-b37151b989bc-with-season-episode-subtitle.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - S3E6 - Madame de Montespan, le grand amour du Roi Soleil")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with episode and subtitle fields`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-episode-subtitle.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - E6 - Madame de Montespan, le grand amour du Roi Soleil")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with episode field`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-episode.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - E6")
                        }
                        .verifyComplete()
            }


            @Test
            fun `with subtitle field`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-subtitle.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - Madame de Montespan, le grand amour du Roi Soleil")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with season and subtitle fields`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-season-and-subtitle.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - S3 - Madame de Montespan, le grand amour du Roi Soleil")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with season field`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-season.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - S3")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with season and episode fields`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-season-episode.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire - S3E6")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with no specific fields`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2d8cde13-4b9d-423f-95c8-b37151b989bc")
                            .willReturn(okJson(fileAsString("/remote/podcast/francetv/v3/items/specific//2d8cde13-4b9d-423f-95c8-b37151b989bc-with-no-specific-fields.json"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(podcast))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.title).isEqualTo("Secrets d'histoire")
                        }
                        .verifyComplete()
            }
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
                        .willReturn(okTextXml(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-no-items.html"))))
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
                        .willReturn(okTextXml(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.with-no-downloadable-items.html"))))
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
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.html"))))
            }

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isEqualTo("82bb91580a61735cce88a83e8d14f925") }
                    .verifyComplete()

            val hash = DigestUtils.md5DigestAsHex("/france-3/secrets-d-histoire/1146545-splendeur-et-decheance-de-lady-hamilton.html-/france-3/secrets-d-histoire/1213409-le-prince-philip-au-service-de-sa-majeste.html-/france-3/secrets-d-histoire/1241451-le-prince-imperial-ou-la-fureur-de-vivre.html-/france-3/secrets-d-histoire/1289373-madame-de-montespan-le-grand-amour-du-roi-soleil.html".toByteArray())
            assertThat(hash).isEqualTo("82bb91580a61735cce88a83e8d14f925")
        }

        @Test
        fun `consistently between two executions`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v3/secrets-d-histoire.html"))))
            }

            val dualSign = Mono.zip(updater.signatureOf(podcast.url), updater.signatureOf(podcast.url))

            /* When */
            StepVerifier.create(dualSign)
                    /* Then */
                    .expectSubscription()
                    .assertNext { (first, second) ->
                        assertThat(first).isEqualTo("82bb91580a61735cce88a83e8d14f925")
                        assertThat(second).isEqualTo("82bb91580a61735cce88a83e8d14f925")
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
