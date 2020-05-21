package com.github.davinkevin.podcastserver.update.updaters.dailymotion

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.update.updaters.CoverFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 13/03/2020
 */
@ExtendWith(SpringExtension::class)
class DailymotionUpdaterTest(
        @Autowired private val updater: DailymotionUpdater
) {

    @TestConfiguration
    @Import(
            WebClientAutoConfiguration::class,
            WebClientConfig::class,
            DailymotionUpdaterConfig::class,
            JacksonAutoConfiguration::class
    )
    class LocalTestConfiguration {
        @Bean fun remapToLocalHost() = remapToMockServer("api.dailymotion.com")
    }

    @MockBean lateinit var imageService: ImageService

    private val podcast = PodcastToUpdate(
            id = UUID.randomUUID(),
            url = URI("https://www.dailymotion.com/karimdebbache"),
            signature = "old_signature"
    )

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        @Test
        fun `with 0 item`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/user/karimdebbache/videos?fields=created_time,description,id,thumbnail_720_url,title")
                    .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.ids.0.item.json")))
            )

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with 1 item`(backend: WireMockServer) {
            /* Given */
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())
            backend.stubFor(get("/user/karimdebbache/videos?fields=created_time,description,id,thumbnail_720_url,title")
                    .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.1.item.json")))
            )

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.url).isEqualTo(URI("https://www.dailymotion.com/video/x5ikng3"))
                        assertThat(it.cover).isEqualTo(CoverFromUpdate(100, 200, URI("https://fake.url.com/img.png")))
                        assertThat(it.title).isEqualTo("CHROMA S01.11 LES AFFRANCHIS")
                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2017, 4, 17, 16, 59, 13, 0, ZoneId.of("Europe/Paris")))
                        assertThat(it.description).isEqualTo("""Avant-dernier épisode de la première saison de CHROMA, sur les Affranchis donc, qui est mon film préféré de tous les temps et que je vous recommande bien sûr d'avoir vu avant de regarder la vidéo. <br /> <br />L'épisode contient également un spoil du Scarface de Brian De Palma. <br /> <br />Et une imitation d'huitre. <br /> <br />Vous pouvez trouver le Bluray des Affranchis (qui contient également le documentaire "Public Enemies: The Golden Age of the Gangster Film") ici : <br />https://www.amazon.fr/Affranchis-%C3%89dition-25%C3%A8me-anniversaire-Digibook/dp/B00T8BY8FU/ref=sr_1_1?ie=UTF8&qid=1492602508&sr=8-1&keywords=affranchis <br /> <br />Le génial livre "En un clin d'oeil" de Walter Murch <br />https://www.amazon.fr/En-clin-doeil-Walter-Murch/dp/2918040304/ref=sr_1_cc_1?s=aps&ie=UTF8&qid=1492602850&sr=1-1-catcorr&keywords=en+un+clin+d%27oeil <br /> <br />Le Bluray de Scarface : <br />https://www.amazon.fr/Scarface-Blu-ray-Al-Pacino/dp/B008K1ZTR0/ref=sr_1_2?s=dvd&ie=UTF8&qid=1492602648&sr=1-2&keywords=scarface <br /> <br />Mean Streets : <br />https://www.amazon.fr/Mean-Streets-%C3%89dition-Collector-Limit%C3%A9e/dp/B004IT5CBQ/ref=sr_1_1?s=dvd&ie=UTF8&qid=1492602959&sr=1-1&keywords=mean+streets <br /> <br />Taxi Driver : <br />https://www.amazon.fr/Taxi-Driver-Blu-ray-Robert-Niro/dp/B004W4P1A4/ref=sr_1_2?s=dvd&ie=UTF8&qid=1492602782&sr=1-2&keywords=taxi+driver <br /> <br />La Couleur de l'argent : <br />https://www.amazon.fr/Couleur-largent-Blu-ray-Paul-Newman/dp/B00BBEGNJ6/ref=sr_1_1?s=dvd&ie=UTF8&qid=1492602983&sr=1-1&keywords=le+couleur+de+l%27argent <br /> <br />Le Temps de l'innocence : <br />https://www.amazon.fr/temps-linnocence-Blu-ray-Daniel-Day-Lewis/dp/B00O3LU7SM/ref=sr_1_2?s=dvd&ie=UTF8&qid=1492603099&sr=1-2&keywords=le+temps+de+l%27innocence <br /> <br />La Dernière Tentation du Christ : <br />https://www.amazon.fr/Derni%C3%A8re-tentation-Christ-Blu-ray/dp/B00BD7LJ0O/ref=sr_1_2?s=dvd&ie=UTF8&qid=1492603140&sr=1-2&keywords=la+derni%C3%A8re+tentation+du+christ <br /> <br />After Hours : <br />https://www.amazon.fr/After-Hours-Griffin-Dunne/dp/B0002V5ZN4/ref=sr_1_1?s=dvd&ie=UTF8&qid=1492603219&sr=1-1&keywords=after+hours <br /> <br />Casino : <br />https://www.amazon.fr/Casino-Blu-ray-Robert-Niro/dp/B001CRVXVU/ref=sr_1_1?s=dvd&ie=UTF8&qid=1492603204&sr=1-1&keywords=casino <br /> <br />Jules et Jim : <br />https://www.amazon.fr/Jules-jim-Jeanne-Moreau/dp/B001GPGXRM/ref=sr_1_1?ie=UTF8&qid=1492602904&sr=8-1&keywords=jules+et+jim <br /> <br />A bout de souffle : <br />https://www.amazon.fr/bout-souffle-Blu-ray-Jean-Seberg/dp/B01IDSEIO4/ref=sr_1_3?s=dvd&ie=UTF8&qid=1492602814&sr=1-3&keywords=a+bout+de+souffle <br /> <br /> <br />La Playlist Spotify de CHROMA : <br />https://open.spotify.com/user/2bash/playlist/6Qj7nQKpOQxJz3Ol00SFop""")
                    }
                    .verifyComplete()
        }

        @Test
        fun `with 10 items`(backend: WireMockServer) {
            /* Given */
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())
            backend.stubFor(get("/user/karimdebbache/videos?fields=created_time,description,id,thumbnail_720_url,title")
                    .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.10.items.json")))
            )

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(10)
                    .verifyComplete()
        }

        @Test
        fun `but fails because url doesn't contain username`() {
            /* Given */
            val p = podcast.copy(url = URI("https://www.dailymotion.com"))
            /* When */
            assertThatThrownBy { updater.findItems(p) }
                    /* Then */
                    .hasMessage("username not found")
        }
    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with 0 item`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/user/karimdebbache/videos?fields=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.ids.0.item.json")))
            )

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("")
                    .verifyComplete()
        }

        @Test
        fun `with 1 item`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/user/karimdebbache/videos?fields=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.ids.1.items.json")))
            )

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("0e2c090cb4f478c4d1c7ab21534e63ff")
                    .verifyComplete()
        }

        @Test
        fun `with 10 items`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/user/karimdebbache/videos?fields=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/dailymotion/karimdebbache.ids.10.items.json")))
            )

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("af103324ed308bbaba8398737ba3fea2")
                    .verifyComplete()
        }

        @Test
        fun `but fails because url doesn't contain username`() {
            /* Given */
            val p = podcast.copy(url = URI("https://www.dailymotion.com"))
            /* When */
            assertThatThrownBy { updater.signatureOf(p.url) }
                    /* Then */
                    .hasMessage("username not found")
        }
    }

    @Test
    fun `should return Dailymotion type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("Dailymotion")
        assertThat(type.name).isEqualTo("Dailymotion")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible`() {
            /* Given */
            val url = "https://www.dailymotion.com/karimdebbache"
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

        @Test
        fun `should not be compatible because url is null`() {
            /* Given */
            val url = null
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }
}
