package com.github.davinkevin.podcastserver.update.updaters.francetv

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
        @Autowired private val updater: FranceTvUpdater,
        @Autowired private val clock: Clock
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

        private fun WireMockServer.forV4(name: String, id: String = "", coverPath: String = "") {
            stubFor(get("/france-3/secrets-d-histoire/$name.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/$name.html"))))

            if(id.isEmpty()) return
            stubFor(get("/v1/videos/$id?country_code=FR&device_type=desktop&browser=chrome")
                    .willReturn(okJson(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/$id.json"))))

            if (coverPath.isEmpty()) return
            stubFor(get(coverPath).willReturn(aResponse().withBodyFile("img/image.png")))
        }

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/no-items.html"))))
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
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
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                forV4("2962423-gustave-flaubert-la-fureur-d-ecrire", "21a395fe-7927-4623-8cee-06a1b76bb08e", "/v1/assets/images/ab/7f/c5/448b7e85-1fdc-4106-8cd1-c67739bf8e41.jpg")
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Secrets d'histoire - Gustave Flaubert, la fureur d'écrire !")
                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2021, 12, 6, 21, 9, 16, 0, ZoneId.of("Europe/Paris")))
                        assertThat(it.length).isNull()
                        assertThat(it.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/2962423-gustave-flaubert-la-fureur-d-ecrire.html"))
                        assertThat(it.description).isEqualTo("Stéphane Bern dresse le portrait de l'écrivain Gustave Flaubert, suivant ses traces à Rouen, sa ville natale, à Paris, où il parfait son art de l'écriture, ou en Orient, où il puise son inspiration. Méticuleux, perfectionniste, fuyant la célébrité, Gustave Flaubert place son oeuvre d'écrivain au-dessus de tout.")
                        assertThat(it.cover!!.height).isEqualTo(300)
                        assertThat(it.cover!!.width).isEqualTo(256)
                        assertThat(it.cover!!.url).isEqualTo(URI("https://assets.webservices.francetelevisions.fr/v1/assets/images/ab/7f/c5/448b7e85-1fdc-4106-8cd1-c67739bf8e41.jpg"))
                    }
                    .verifyComplete()
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/2962423-gustave-flaubert-la-fureur-d-ecrire.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/2962423-gustave-flaubert-la-fureur-d-ecrire.html"))))

                stubFor(get("/v1/videos/21a395fe-7927-4623-8cee-06a1b76bb08e?country_code=FR&device_type=desktop&browser=chrome")
                    .willReturn(okJson(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/21a395fe-7927-4623-8cee-06a1b76bb08e.json"))))

                stubFor(get("/v1/assets/images/ab/7f/c5/448b7e85-1fdc-4106-8cd1-c67739bf8e41.jpg")
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
        fun `with error on api backend`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/one-item.html"))))

                stubFor(get("/france-3/secrets-d-histoire/2962423-gustave-flaubert-la-fureur-d-ecrire.html")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/2962423-gustave-flaubert-la-fureur-d-ecrire.html"))))

                stubFor(get("/v1/videos/21a395fe-7927-4623-8cee-06a1b76bb08e?country_code=FR&device_type=desktop&browser=chrome")
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/all.html"))))

                forV4("558531-marie-de-medicis-ou-l-obsession-du-pouvoir", "afe92eeb-5a53-4e77-8947-2e264aa66692", "/v1/assets/images/62/f2/98/720bff7b-4bf1-46d3-887d-b43196616b3e.jpeg")
                forV4("2292725-josephine-baker-la-fleur-au-fusil", "e161817c-5053-44df-9b2b-262f4a1fadd2", "/v1/assets/images/72/e5/03/39c59a49-42aa-4c85-bdbc-50f05ebd795b.jpg")
                forV4("1420601-therese-la-petite-sainte-de-lisieux", "0476750f-e627-4170-9861-923f45ec33d6", "/v1/assets/images/46/0c/10/13b811d8-bc75-4d0d-b081-4b7ce4b435f0.jpg")
                forV4("2882325-emile-zola-la-verite-quoi-qu-il-en-coute", "677e551f-4a7c-4007-824d-399b2d8666f7", "/v1/assets/images/0c/84/fb/6f5cba0a-4093-47e8-9072-0f7890c8d379.jpg")
                forV4("2962423-gustave-flaubert-la-fureur-d-ecrire", "21a395fe-7927-4623-8cee-06a1b76bb08e", "/v1/assets/images/ab/7f/c5/448b7e85-1fdc-4106-8cd1-c67739bf8e41.jpg")
                forV4("1372235-splendeur-et-decheance-de-lady-hamilton", "663c0872-242d-4fbe-a584-520625185b58", "/v1/assets/images/64/1c/41/d4d4d809-2ad7-49ad-990e-0119c2c9042e.jpeg")
                forV4("1047181-jean-de-la-fontaine", "3cfa0c37-8e2b-4859-9376-903dadf765ba", "/v1/assets/images/7a/d2/70/9657ce30-3027-4928-b311-93de116f4b40.jpeg")
                forV4("2121097-beethoven-tout-pour-la-musique", "81f6319f-b4c0-48d4-905f-c0133bab84c4", "/v1/assets/images/0a/ed/37/3795743d-0e2b-47ba-b212-173b0001207f.jpeg")
                forV4("2210295-josephine-l-atout-irresistible-de-napoleon", "0fd90d24-fbb6-4c2c-bbb1-6306306a2fa6", "/v1/assets/images/28/e2/19/0dc1badc-edec-496a-bdee-76c11760da7b.jpeg")
                forV4("1947399-guillaume-le-conquerant-a-nous-deux-l-angleterre", "c58df3b5-48f7-467d-9477-9535752c733f", "/v1/assets/images/5a/6a/9a/1bef76dc-4e36-4ccd-9ab3-4043d88d929b.jpeg")
                forV4("2221543-desiree-clary-marseillaise-et-reine-de-suede", "afd1e541-3491-4264-a8ab-403c287d8517", "/v1/assets/images/3c/1f/dc/c8497a56-19f7-454c-b9bc-d35a7b8b03e6.jpeg")
                forV4("2801349-les-secrets-des-templiers", "bb486b66-1ad6-11ec-93de-000d3a23d482", "/v1/assets/images/5d/b2/13/397fa51e-40b8-4767-8614-840f39c4d6c1.jpeg")
                forV4("2212739-lucrece-borgia-une-femme-au-vatican", "5722430e-280d-42b0-968b-c856b8a01cda", "/v1/assets/images/02/ef/a7/c5ccc968-fe19-4fd2-9294-4532a7eb9f54.jpeg")
                forV4("16913-secrets-d-histoire-les-femmes-de-la-revolution", "915bdd92-c3b9-411e-935b-23249f0929f3", "/v1/assets/images/ba/18/d0/4b60ed22-6293-44ac-ad7f-83d150d9a7fc.jpeg")
                forV4("2212737-agrippine-tu-seras-un-monstre-mon-fils", "50b4894b-1e64-466f-a462-34b93d933f77", "/v1/assets/images/54/41/82/a5bba5ca-70e8-41af-8c1e-143009c588c5.jpeg")
                forV4("1372237-madame-de-montespan-le-grand-amour-du-roi-soleil", "652c1cce-b67c-4912-9462-081e894f864d", "/v1/assets/images/a5/80/d6/b1d9b3b5-93f6-48b1-af68-af6e5c95f402.jpg")
                forV4("1241451-le-prince-imperial-ou-la-fureur-de-vivre", "5bd64ea4-9b14-46a1-a730-073761ea98f7", "/v1/assets/images/1f/41/a5/e83b492c-bba1-417b-8e2e-90c482073193.jpg")

            }

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(17)
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/no-items.html"))))
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
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/all-unavailable.html"))))
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
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isEqualTo("b3ac240ba14117df7f2d368bc7090952") }
                    .verifyComplete()

            val hash = DigestUtils.md5DigestAsHex("/france-3/secrets-d-histoire/1047181-jean-de-la-fontaine.html-/france-3/secrets-d-histoire/1241451-le-prince-imperial-ou-la-fureur-de-vivre.html-/france-3/secrets-d-histoire/1372235-splendeur-et-decheance-de-lady-hamilton.html-/france-3/secrets-d-histoire/1372237-madame-de-montespan-le-grand-amour-du-roi-soleil.html-/france-3/secrets-d-histoire/1420601-therese-la-petite-sainte-de-lisieux.html-/france-3/secrets-d-histoire/16913-secrets-d-histoire-les-femmes-de-la-revolution.html-/france-3/secrets-d-histoire/1947399-guillaume-le-conquerant-a-nous-deux-l-angleterre.html-/france-3/secrets-d-histoire/2121097-beethoven-tout-pour-la-musique.html-/france-3/secrets-d-histoire/2210295-josephine-l-atout-irresistible-de-napoleon.html-/france-3/secrets-d-histoire/2212737-agrippine-tu-seras-un-monstre-mon-fils.html-/france-3/secrets-d-histoire/2212739-lucrece-borgia-une-femme-au-vatican.html-/france-3/secrets-d-histoire/2221543-desiree-clary-marseillaise-et-reine-de-suede.html-/france-3/secrets-d-histoire/2292725-josephine-baker-la-fleur-au-fusil.html-/france-3/secrets-d-histoire/2801349-les-secrets-des-templiers.html-/france-3/secrets-d-histoire/2882325-emile-zola-la-verite-quoi-qu-il-en-coute.html-/france-3/secrets-d-histoire/2962423-gustave-flaubert-la-fureur-d-ecrire.html-/france-3/secrets-d-histoire/558531-marie-de-medicis-ou-l-obsession-du-pouvoir.html".toByteArray())
            assertThat(hash).isEqualTo("b3ac240ba14117df7f2d368bc7090952")
        }

        @Test
        fun `consistent between two executions`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/toutes-les-videos/")
                    .willReturn(ok(fileAsString("/remote/podcast/francetv/v4/secrets-d-histoire/toutes-les-videos/all.html"))))
            }

            val dualSign = Mono.zip(updater.signatureOf(podcast.url), updater.signatureOf(podcast.url))

            /* When */
            StepVerifier.create(dualSign)
                    /* Then */
                    .expectSubscription()
                    .assertNext { (first, second) ->
                        assertThat(first).isEqualTo("b3ac240ba14117df7f2d368bc7090952")
                        assertThat(second).isEqualTo("b3ac240ba14117df7f2d368bc7090952")
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
