package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
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
        @Bean fun webClientBuilder() = WebClient.builder()
                .filter(remapToMockServer("www.france.tv"))
                .filter(remapToMockServer("sivideo.webservices.francetelevisions.fr"))
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

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(okTextXml(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2_with_no_items.html"))))
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with one item`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2.with-one-item.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948775-immersion-dans-le-mystere-toutankhamon.html"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=c59c33ea-507b-11e9-b0a1-000d3a2437a2")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/c59c33ea-507b-11e9-b0a1-000d3a2437a2.json"))))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-27/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682841.jpeg")
                        .willReturn(aResponse().withBodyFile("img/image.png")))
            }
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Immersion dans le mystère Toutankhamon")
                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2019, 3, 27, 11, 25, 44, 0, ZoneId.of("Europe/Paris")))
                        assertThat(it.length).isNull()
                        assertThat(it.url).isEqualTo(URI("https://www.france.tv/france-3/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"))
                        assertThat(it.description).isEqualTo("C'est en ouvrant le sarcophage de Toutânkhamon qu'Howard Carter fit la découverte archéologique la plus célèbre de l'Histoire... Un siècle après, le mystère s'est-il éclairci ?")
                        assertThat(it.cover!!.height).isEqualTo(300)
                        assertThat(it.cover!!.width).isEqualTo(256)
                        assertThat(it.cover!!.url).isEqualTo(URI("https://sivideo.webservices.francetelevisions.fr/staticftv/images_pdm_ni/2019-03-27/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682841.jpeg"))
                    }
                    .verifyComplete()
        }

        @Test
        fun `with one item without cover`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2.with-one-item-without-cover.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948083-la-decouverte-de-la-tombe-de-toutankhamon-en-novembre-1922.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948083-la-decouverte-de-la-tombe-de-toutankhamon-en-novembre-1922.html"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=9e989bce-4f56-11e9-80e8-000d3a2439ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/9e989bce-4f56-11e9-80e8-000d3a2439ea.json"))))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-26/9e989bce-4f56-11e9-80e8-000d3a2439ea_1553556918.jpeg").willReturn(notFound()))
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
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2.with-one-item-without-cover.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948083-la-decouverte-de-la-tombe-de-toutankhamon-en-novembre-1922.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948083-la-decouverte-de-la-tombe-de-toutankhamon-en-novembre-1922.html"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=9e989bce-4f56-11e9-80e8-000d3a2439ea")
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
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2.html"))))
                stubFor(get("/france-3/secrets-d-histoire/670251-la-legende-noire-de-la-reine-margot.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/670251-la-legende-noire-de-la-reine-margot.html"))))
                stubFor(get("/france-3/secrets-d-histoire/721907-cuisine-et-cave-du-chateau-de-cazeneuve.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/721907-cuisine-et-cave-du-chateau-de-cazeneuve.html"))))
                stubFor(get("/france-3/secrets-d-histoire/762535-louis-philippe-et-marie-amelie-notre-dernier-couple-royal.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/762535-louis-philippe-et-marie-amelie-notre-dernier-couple-royal.html"))))
                stubFor(get("/france-3/secrets-d-histoire/762571-marie-therese-l-envahissante-imperatrice-d-autriche.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/762571-marie-therese-l-envahissante-imperatrice-d-autriche.html"))))
                stubFor(get("/france-3/secrets-d-histoire/762573-le-duc-d-aumale-le-magicien-de-chantilly.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/762573-le-duc-d-aumale-le-magicien-de-chantilly.html"))))
                stubFor(get("/france-3/secrets-d-histoire/781001-louis-philippe-et-marie-amelie-notre-dernier-couple-royal-bande-annonce.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/781001-louis-philippe-et-marie-amelie-notre-dernier-couple-royal-bande-annonce.html"))))
                stubFor(get("/france-3/secrets-d-histoire/785527-chambre-de-la-reine-des-belges-au-grand-trianon.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/785527-chambre-de-la-reine-des-belges-au-grand-trianon.html"))))
                stubFor(get("/france-3/secrets-d-histoire/785529-avec-la-fayette-au-balcon-de-l-hotel-de-ville.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/785529-avec-la-fayette-au-balcon-de-l-hotel-de-ville.html"))))
                stubFor(get("/france-3/secrets-d-histoire/785531-les-aventures-en-exil.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/785531-les-aventures-en-exil.html"))))
                stubFor(get("/france-3/secrets-d-histoire/785569-la-revolution-de-1848-et-l-abdication-de-louis-philippe.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/785569-la-revolution-de-1848-et-l-abdication-de-louis-philippe.html"))))
                stubFor(get("/france-3/secrets-d-histoire/785571-l-enfance-et-l-education-de-louis-philippe.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/785571-l-enfance-et-l-education-de-louis-philippe.html"))))
                stubFor(get("/france-3/secrets-d-histoire/848453-philippe-d-orleans-le-regent-libertin.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/848453-philippe-d-orleans-le-regent-libertin.html"))))
                stubFor(get("/france-3/secrets-d-histoire/848457-la-du-barry-coup-de-foudre-a-versailles.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/848457-la-du-barry-coup-de-foudre-a-versailles.html"))))
                stubFor(get("/france-3/secrets-d-histoire/931241-ramses-ii-toutankhamon-l-egypte-des-pharaons.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/931241-ramses-ii-toutankhamon-l-egypte-des-pharaons.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948051-le-temple-de-louxor-et-les-statues-de-ramses-ii.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948051-le-temple-de-louxor-et-les-statues-de-ramses-ii.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948061-le-tresor-de-toutankhamon-au-musee-du-caire.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948061-le-tresor-de-toutankhamon-au-musee-du-caire.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948067-la-momie-de-ramses-ii.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948067-la-momie-de-ramses-ii.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948069-le-temple-d-abou-simbel-a-la-gloire-de-ramses.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948069-le-temple-d-abou-simbel-a-la-gloire-de-ramses.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948083-la-decouverte-de-la-tombe-de-toutankhamon-en-novembre-1922.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948083-la-decouverte-de-la-tombe-de-toutankhamon-en-novembre-1922.html"))))
                stubFor(get("/france-3/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/948775-immersion-dans-le-mystere-toutankhamon.html"))))

                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=2a05e884-4f42-11e9-90bb-000d3a2427ab")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/2a05e884-4f42-11e9-90bb-000d3a2427ab.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=3d22342a-b2c3-4904-becf-bb47d1b66205")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/3d22342a-b2c3-4904-becf-bb47d1b66205.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=4c6c8b60-9853-4c1b-bac7-2c114c798b90")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/4c6c8b60-9853-4c1b-bac7-2c114c798b90.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=5a3b3e8c-4f4c-11e9-a960-000d3a23d482")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/5a3b3e8c-4f4c-11e9-a960-000d3a23d482.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=5c125e1c-d93f-11e8-8e96-000d3a2439ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/5c125e1c-d93f-11e8-8e96-000d3a2439ea.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=5ce49435-cbed-4023-8af3-fa7bb40ffe2c")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/5ce49435-cbed-4023-8af3-fa7bb40ffe2c.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=5da6d37c-c168-4cc0-8892-87c2e2c44657")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/5da6d37c-c168-4cc0-8892-87c2e2c44657.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=7c509f8c-c2f6-4b15-b730-958c13a457ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/7c509f8c-c2f6-4b15-b730-958c13a457ea.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=7fb4cf72-4f36-11e9-94f6-000d3a2439ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/7fb4cf72-4f36-11e9-94f6-000d3a2439ea.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=9e989bce-4f56-11e9-80e8-000d3a2439ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/9e989bce-4f56-11e9-80e8-000d3a2439ea.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=14cb2278-d93f-11e8-b236-000d3a23d482")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/14cb2278-d93f-11e8-b236-000d3a23d482.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=333ae826-e0b6-4322-a079-a30519695cd0")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/333ae826-e0b6-4322-a079-a30519695cd0.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=947fe79c-d93f-11e8-9671-000d3a2437a2")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/947fe79c-d93f-11e8-9671-000d3a2437a2.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=750688f6-d789-11e8-b3c7-000d3a2437a2")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/750688f6-d789-11e8-b3c7-000d3a2437a2.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=9278030b-5310-4568-b082-60972d9bb5ec")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/9278030b-5310-4568-b082-60972d9bb5ec.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=a0355412-4f2f-11e9-90bb-000d3a2427ab")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/a0355412-4f2f-11e9-90bb-000d3a2427ab.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=c59c33ea-507b-11e9-b0a1-000d3a2437a2")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/c59c33ea-507b-11e9-b0a1-000d3a2437a2.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=c380cce2-d93e-11e8-8e96-000d3a2439ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/c380cce2-d93e-11e8-8e96-000d3a2439ea.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=d735b918-d93f-11e8-82cc-000d3a23d482")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/d735b918-d93f-11e8-82cc-000d3a23d482.json"))))
                stubFor(get("/tools/getInfosOeuvre/v2/?idDiffusion=f4e6ae6e-b1bb-11e8-ba5d-000d3a2439ea")
                        .willReturn(okJson(fileAsString("/remote/podcast/francetv/f4e6ae6e-b1bb-11e8-ba5d-000d3a2439ea.json"))))

                val defaultCover = aResponse().withBodyFile("img/image.png")
                stubFor(get("/staticftv/images_pdm_ni/2019-03-25/2a05e884-4f42-11e9-90bb-000d3a2427ab_1553548139.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2018-10-30/EMI_364799.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2018-10-30/EMI_760280.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-25/5a3b3e8c-4f4c-11e9-a960-000d3a23d482_1553552514.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-10-26/5c125e1c-d93f-11e8-8e96-000d3a2439ea_1540572700.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2019-01-08/EMI_867052.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2018-09-06/EMI_758331.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2018-10-30/PDM_191496304.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-25/7fb4cf72-4f36-11e9-94f6-000d3a2439ea_1553543121.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-26/9e989bce-4f56-11e9-80e8-000d3a2439ea_1553556918.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-10-26/14cb2278-d93f-11e8-b236-000d3a23d482_1540572579.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2019-03-23/EMI_885707.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-10-26/947fe79c-d93f-11e8-9671-000d3a2437a2_1540572791.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-10-24/750688f6-d789-11e8-b3c7-000d3a2437a2_1540384609.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/ref_emissions/2019-01-08/EMI_767671.jpg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-25/a0355412-4f2f-11e9-90bb-000d3a2427ab_1553540174.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2019-03-27/c59c33ea-507b-11e9-b0a1-000d3a2437a2_1553682841.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-10-26/c380cce2-d93e-11e8-8e96-000d3a2439ea_1540572444.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-10-26/d735b918-d93f-11e8-82cc-000d3a23d482_1540572896.jpeg").willReturn(defaultCover))
                stubFor(get("/staticftv/images_pdm_ni/2018-09-06/f4e6ae6e-b1bb-11e8-ba5d-000d3a2439ea_1536228164.jpeg").willReturn(defaultCover))
            }

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(20)
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
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(okTextXml(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2_with_no_items.html"))))
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
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2.html"))))
            }

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isEqualTo("cdd2cd116fb2727d573ed6c12a3718e3") }
                    .verifyComplete()
        }

        @Test
        fun `with all items consistent`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("/france-3/secrets-d-histoire/replay-videos/ajax/?page=0")
                        .willReturn(ok(fileAsString("/remote/podcast/francetv/secrets-d-histoire.v2.html"))))
            }

            val dualSign = Mono.zip(updater.signatureOf(podcast.url), updater.signatureOf(podcast.url))

            /* When */
            StepVerifier.create(dualSign)
                    /* Then */
                    .expectSubscription()
                    .assertNext { (first, second) ->
                        assertThat(first).isEqualTo("cdd2cd116fb2727d573ed6c12a3718e3")
                        assertThat(second).isEqualTo("cdd2cd116fb2727d573ed6c12a3718e3")
                    }
                    .verifyComplete()
        }
    }

    @Test
    fun `should return rss type`() {
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

    @Nested
    @DisplayName("blocking")
    inner class Blocking {

        @Test
        fun `should not serve items with blocking method`() {
            /* Given */
            /* When */
            assertThatThrownBy { updater.blockingFindItems(podcast) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }

        @Test
        fun `should not sign podcast with blocking method`() {
            /* Given */
            /* When */
            assertThatThrownBy { updater.blockingSignatureOf(podcast.url) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }
    }

}
