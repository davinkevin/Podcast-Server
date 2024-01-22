package com.github.davinkevin.podcastserver.extension.podcastserver.item

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.stream.Stream

class SluggableTest {

    @DisplayName("should return")
    @MethodSource("specificCases")
    @ParameterizedTest(name = "should manage title with {0}")
    fun `should manage title with`(original: String, expectation: String) {
        /* Given */
        val item = FakeItem(original, "audio/mp3")

        /* When */
        val slug = item.slug()

        /* Then */
        assertThat(slug).isEqualTo("$expectation.mp3")
    }

    companion object {
        @JvmStatic
        fun specificCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of("Le cri du cœur : Youssef Krou et Arnaud Gauthier-Rat invités du Super Moscato Show - 22/01", "le-cri-du-c_ur-youssef-krou-et-arnaud-gauthier-rat-invites-du-super-moscato-show-22-01"),
                Arguments.of("Alien³ (avec Océane Zerbini et Aurélien Noyer)", "alien_-avec-oceane-zerbini-et-aurelien-noyer"),
                Arguments.of("\uD83C\uDDE8\uD83C\uDDE6 Montréal & Québec : Entre paysages, histoire et gastronomie", "__-montreal-quebec-entre-paysages-histoire-et-gastronomie"),
                Arguments.of("Gatsby le Magnifique | avec 2 Heures de Perdues", "gatsby-le-magnifique-_-avec-2-heures-de-perdues"),
                Arguments.of("RDV Tech 539 - La boite de TicTac à 600€ - AI Pin, Instagram payant, channels Whatsapp, Apple Tap to Pay, Uber Tasks", "rdv-tech-539-la-boite-de-tictac-a-600_-ai-pin-instagram-payant-channels-whatsapp-apple-tap-to-pay-uber-tasks"),
                Arguments.of("Java™ 19 & 20. What's new and noteworthy? (Piotr PRZYBYL)\n", "java_-19-20-what-s-new-and-noteworthy-piotr-przybyl"),

            )
    }


}

data class FakeItem(override val title: String, override val mimeType: String, override val fileName: Path? = null): Sluggable {}