package com.github.davinkevin.podcastserver.business.stats

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

/**
 * Created by kevin on 01/07/15 for Podcast Server
 */
class NumberOfItemByDateWrapperTest {

    private val numberOfItemByDateWrapper = NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 100)

    @Test
    fun `should have the correct value`() {
        assertThat(numberOfItemByDateWrapper.date).isEqualTo(LocalDate.of(2015, Month.JULY, 1))
        assertThat(numberOfItemByDateWrapper.numberOfItems).isEqualTo(100)
    }

    @Test
    fun `should be equals and have the same hashcode`() {
        /* Given */
        val aCopy = NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 200)
        /* Then */
        assertThat(numberOfItemByDateWrapper).isEqualTo(aCopy)
        assertThat(numberOfItemByDateWrapper.hashCode()).isEqualTo(aCopy.hashCode())
    }
}
