package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
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

        private fun WireMockServer.forV5(path: String, id: String = "", coverPath: String = "") {
            stubFor(get(path)
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/videos/${path.substringAfterLast("/")}"))))

            if(id.isEmpty()) return
            stubFor(get("/v1/videos/$id?country_code=FR&device_type=desktop&browser=chrome")
                    .willReturn(okJson(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/videos/$id.json"))))

            if (coverPath.isEmpty()) return
            stubFor(get(coverPath).willReturn(aResponse().withBodyFile("img/image.png")))
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/no-item.html"))))
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
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
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV5(
                    path = "/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/4228084-ragnar-le-viking-qui-a-terrorise-paris.html",
                    id = "285f5fbe-7cee-4aaf-a661-cf5b91198576",
                    coverPath = "/v1/assets/images/37/0f/7f/f8a323c4-faeb-4db8-a8d7-5932117dc7bf.jpg"
                )
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Secrets d'histoire - Ragnar, le viking qui a terrorisé Paris")
                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2022, 10, 31, 21, 9, 0, 0, ZoneId.of("Europe/Paris")))
                        assertThat(it.length).isNull()
                        assertThat(it.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/4228084-ragnar-le-viking-qui-a-terrorise-paris.html"))
                        assertThat(it.description).isEqualTo("Du IXe au XIe siècle, une vague de violence inouïe s'abat sur l'Europe tout entière. Les Vikings pillent et dévastent les monastères et les villages des royaumes qu'ils traversent. En quelques minutes, un havre de paix se transforme en une scène d'apocalypse. Comme ce jour où ils décident d'envahir Paris. Qui sont ces guerriers venus de Scandinavie ? Si leur réputation de barbares sanguinaires les précède, leur culture est nettement moins connue et bien plus raffinée que ce que l'on a longtemps pensé. Stéphane Bern se rend en Norvège, sur les rivages du Naeroyfjord, considéré comme l'un des plus beaux fjords au monde et classé au patrimoine mondial de l'Unesco. C'est ici, dans les pays scandinaves, que tout commence pour les Vikings, avec le plus célèbre de leurs chefs : Ragnar Lodbrok.")
                        assertThat(it.cover!!.height).isEqualTo(300)
                        assertThat(it.cover!!.width).isEqualTo(256)
                        assertThat(it.cover!!.url).isEqualTo(URI("https://assets.webservices.francetelevisions.fr/v1/assets/images/37/0f/7f/f8a323c4-faeb-4db8-a8d7-5932117dc7bf.jpg"))
                    }
                    .verifyComplete()
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                val name = "4228084-ragnar-le-viking-qui-a-terrorise-paris"
                val id = "285f5fbe-7cee-4aaf-a661-cf5b91198576"
                val coverPath = "/v1/assets/images/37/0f/7f/f8a323c4-faeb-4db8-a8d7-5932117dc7bf.jpg"

                stubFor(get("/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/$name.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/videos/$name.html"))))

                stubFor(get("/v1/videos/$id?country_code=FR&device_type=desktop&browser=chrome")
                    .willReturn(okJson(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/videos/$id.json"))))

                stubFor(get(coverPath).willReturn(notFound()))
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
        fun `with error on api backend`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                val name = "4228084-ragnar-le-viking-qui-a-terrorise-paris"
                val id = "285f5fbe-7cee-4aaf-a661-cf5b91198576"

                stubFor(get("/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/$name.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/videos/$name.html"))))

                stubFor(get("/v1/videos/$id?country_code=FR&device_type=desktop&browser=chrome")
                    .willReturn(serverError()))
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/all.html"))))

                forV5(
                    path = "/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/4228084-ragnar-le-viking-qui-a-terrorise-paris.html",
                    id = "285f5fbe-7cee-4aaf-a661-cf5b91198576",
                    coverPath = "/v1/assets/images/37/0f/7f/f8a323c4-faeb-4db8-a8d7-5932117dc7bf.jpg"
                )
                forV5(
                    path = "/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/4206502-rosa-bonheur-la-fee-des-animaux.html",
                    id = "95358a72-5487-4009-962e-0d130ee9bbdb",
                    coverPath = "/v1/assets/images/9a/a8/18/777c2929-be8e-4afa-91c5-8b8ee8150f65.jpg"
                )
                forV5(
                    path = "/sport/les-jeux-olympiques/4211497-paris-2024-stephane-bern-livre-les-secrets-d-une-marche-historique.html",
                    id = "86edd5a8-44bb-11ed-9829-c78752ac9b2b",
                    coverPath = "/v1/assets/images/10/85/66/0e2d05b6-fff2-4923-89f8-1e8f7ba854eb.jpg"
                )
                forV5(
                    path = "/france-3/secrets-d-histoire/4116544-la-reine-elizabeth-ii-et-sa-relation-a-la-france.html",
                    id = "463e7292-3430-11ed-b4a9-c3ed945d3889",
                    coverPath = "/v1/assets/images/9c/16/58/0040ec2a-b383-4376-95dc-fc3e66f86703.jpg"
                )
                forV5(
                    path = "/france-3/secrets-d-histoire/4073119-medecin-il-a-essaye-de-sauver-lady-diana-le-soir-de-l-accident.html",
                    id = "d8bff46a-2913-11ed-8457-f3708da52c87",
                    coverPath = "/v1/assets/images/48/2f/7e/601b72ac-b05f-450a-9ce7-2def8e7c135c.jpg"
                )
                forV5(
                    path = "/france-3/secrets-d-histoire/4073167-les-engagements-humanitaires-de-lady-di.html",
                    id = "50eaca2c-291f-11ed-8b9b-19c64cfd8302",
                    coverPath = "/v1/assets/images/79/b4/30/2e4bffbb-7306-4cb8-b3fa-82302abab38d.jpg"
                )
                forV5(
                    path = "/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/3786031-diana-cette-illustre-inconnue.html",
                    id = "d16b46b5-792d-4327-8991-7242ab1279aa",
                    coverPath = "/v1/assets/images/74/bf/23/38feea9d-3c0c-4509-a2ac-f56213532377.jpg"
                )
            }

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(7)
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/no-item.html"))))
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
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
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isEqualTo("0f519fa4a84d305e1f297476b36935a3") }
                    .verifyComplete()

            val hash = DigestUtils.md5DigestAsHex("/france-3/secrets-d-histoire/4073119-medecin-il-a-essaye-de-sauver-lady-diana-le-soir-de-l-accident.html-/france-3/secrets-d-histoire/4073167-les-engagements-humanitaires-de-lady-di.html-/france-3/secrets-d-histoire/4116544-la-reine-elizabeth-ii-et-sa-relation-a-la-france.html-/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/3786031-diana-cette-illustre-inconnue.html-/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/4206502-rosa-bonheur-la-fee-des-animaux.html-/france-3/secrets-d-histoire/secrets-d-histoire-saison-16/4228084-ragnar-le-viking-qui-a-terrorise-paris.html-/sport/les-jeux-olympiques/4211497-paris-2024-stephane-bern-livre-les-secrets-d-une-marche-historique.html".toByteArray())
            assertThat(hash).isEqualTo("0f519fa4a84d305e1f297476b36935a3")
        }

        @Test
        fun `consistent between two executions`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v5/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            val dualSign = Mono.zip(updater.signatureOf(podcast.url), updater.signatureOf(podcast.url))

            /* When */
            StepVerifier.create(dualSign)
                    /* Then */
                    .expectSubscription()
                    .assertNext { (first, second) ->
                        assertThat(first).isEqualTo("0f519fa4a84d305e1f297476b36935a3")
                        assertThat(second).isEqualTo("0f519fa4a84d305e1f297476b36935a3")
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
