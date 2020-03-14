package com.github.davinkevin.podcastserver.find.finders.gulli

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

@ExtendWith(SpringExtension::class)
class GulliFinderTest(
        @Autowired val finder: GulliFinder
) {

    @MockBean lateinit var image: ImageService

    @Nested
    @DisplayName("should find")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        @Test
        fun `podcast by url`(backend: WireMockServer) {
            /* Given */
            val podcastUrl = "http://replay.gulli.fr/dessins-animes/Pokemon3"
            val coverUrl = "https://resize-gulli.jnsmedia.fr/rcrop/1900,550,smartcrop,center-top/img//var/jeunesse/storage/images/gulli/chaine-tv/dessins-animes/pokemon/23289795-181-fre-FR/Pokemon.jpg"

            whenever(image.fetchCoverInformation(URI(coverUrl)))
                    .thenReturn(CoverInformation(123, 456, URI(coverUrl)).toMono())

            backend.stubFor(get("/dessins-animes/Pokemon3")
                    .willReturn(ok(fileAsString("/remote/podcast/gulli/pokemon.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(podcastUrl))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Pokémon")
                        assertThat(it.url).isEqualTo(URI(podcastUrl))
                        assertThat(it.type).isEqualTo("Gulli")
                        assertThat(it.description).isEqualTo("Saison 22 - Pokémon, la série : Soleil et Lune –Ultra-Légendes Alors que Sacha a réussi trois des quatre Grandes Épreuves de la région d’Alola, de nouvelles aventures se profilent pour lui et ses amis. Ils font l’acquisition de Cristaux Z, se lient d’amitié avec des Pokémon et apprennent à faire du Surf Démanta. En tant qu’Ultra-Gardiens, les étudiants de l’École Pokémon acceptent de protéger le Mont Wela. Sacha rencontre un nouveau rival, Tili, et son Efflèche qui offre à Brindibou un défi de taille. Motisma-Dex vit lui aussi une aventure lorsque nos héros découvrent les coulisses de son programme télévisé favori. Quant au Professeur Euphorbe, son rêve de commencer une Ligue Pokémon à Alola pourrait bien devenir réalité...")
                        assertThat(it.cover).isEqualTo(FindCoverInformation(
                                height = 456,
                                width = 123,
                                url = URI(coverUrl)
                        ))
                    }
                    .verifyComplete()
        }

        @Test
        fun `podcast without cover`(backend: WireMockServer) {
            /* Given */
            val podcastUrl = "http://replay.gulli.fr/dessins-animes/Pokemon3"

            backend.stubFor(get("/dessins-animes/Pokemon3")
                    .willReturn(ok(fileAsString("/remote/podcast/gulli/pokemon.without-cover.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(podcastUrl))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Pokémon")
                        assertThat(it.url).isEqualTo(URI(podcastUrl))
                        assertThat(it.type).isEqualTo("Gulli")
                        assertThat(it.description).isEqualTo("Saison 22 - Pokémon, la série : Soleil et Lune –Ultra-Légendes Alors que Sacha a réussi trois des quatre Grandes Épreuves de la région d’Alola, de nouvelles aventures se profilent pour lui et ses amis. Ils font l’acquisition de Cristaux Z, se lient d’amitié avec des Pokémon et apprennent à faire du Surf Démanta. En tant qu’Ultra-Gardiens, les étudiants de l’École Pokémon acceptent de protéger le Mont Wela. Sacha rencontre un nouveau rival, Tili, et son Efflèche qui offre à Brindibou un défi de taille. Motisma-Dex vit lui aussi une aventure lorsque nos héros découvrent les coulisses de son programme télévisé favori. Quant au Professeur Euphorbe, son rêve de commencer une Ligue Pokémon à Alola pourrait bien devenir réalité...")
                        assertThat(it.cover).isNull()
                    }
                    .verifyComplete()
        }

        @Test
        fun `podcast without description`(backend: WireMockServer) {
            /* Given */
            val podcastUrl = "http://replay.gulli.fr/dessins-animes/Pokemon3"
            val coverUrl = "https://resize-gulli.jnsmedia.fr/rcrop/1900,550,smartcrop,center-top/img//var/jeunesse/storage/images/gulli/chaine-tv/dessins-animes/pokemon/23289795-181-fre-FR/Pokemon.jpg"

            whenever(image.fetchCoverInformation(URI(coverUrl)))
                    .thenReturn(CoverInformation(123, 456, URI(coverUrl)).toMono())

            backend.stubFor(get("/dessins-animes/Pokemon3")
                    .willReturn(ok(fileAsString("/remote/podcast/gulli/pokemon.without-description.html"))))

            /* When */
            StepVerifier.create(finder.findInformation(podcastUrl))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Pokémon")
                        assertThat(it.url).isEqualTo(URI(podcastUrl))
                        assertThat(it.type).isEqualTo("Gulli")
                        assertThat(it.description).isEqualTo("")
                        assertThat(it.cover).isEqualTo(FindCoverInformation(
                                height = 456,
                                width = 123,
                                url = URI(coverUrl)
                        ))
                    }
                    .verifyComplete()
        }


    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://replay.gulli.fr/dessins-animes/Sonic-Boom",
        "https://replay.gulli.fr/dessins-animes/Pokemon7/",
        "https://replay.gulli.fr/series/En-Famille",
        "https://replay.gulli.fr/emissions/Gu-Live36"
    ])
    fun `should be compatible with `(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @DisplayName("shoud not be compatible")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "https://www.france2.tv/france-2/vu/",
        "https://www.foo.com/france-2/vu/",
        "https://www.mycanal.fr/france-2/vu/",
        "https://www.6play.fr/france-2/vu/"
    ])
    fun `should not be compatible`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `should not be compatible with null value`() {
        /* Given */
        /* When */
        val compatibility = finder.compatibility(null)
        /* Then */
        assertThat(compatibility).isEqualTo(Int.MAX_VALUE)
    }

    @TestConfiguration
    @Import(GulliFinderConfig::class, WebClientAutoConfiguration::class, JacksonAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration {
        @Bean fun remapToMockServer() = remapToMockServer("replay.gulli.fr")

    }
}
