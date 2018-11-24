package com.github.davinkevin.podcastserver.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Created by kevin on 14/06/15 for HackerRank problem
 */
class StatusTest {

    @Test
    fun `should check value`() {
        assertThat(Status.of("NOT_DOWNLOADED"))
                .isEqualTo(Status.NOT_DOWNLOADED)
    }

    @Test
    fun `should throw exception`() {
        assertThatThrownBy { Status.of("") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("No enum constant Status.")
    }
}
