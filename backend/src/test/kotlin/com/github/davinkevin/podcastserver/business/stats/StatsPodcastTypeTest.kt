package com.github.davinkevin.podcastserver.business.stats

import io.vavr.API.Set
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

/**
 * Created by kevin on 01/07/15 for Podcast Server
 */
class StatsPodcastTypeTest {

    @Test
    fun `should has correct value`() {
        /* Given */
        val numberOfItemSets = Set(NI_1, NI_2, NI_3)

        /* When */
        val (type, values) = StatsPodcastType(FAKE_TYPE, numberOfItemSets)

        /* Then */
        assertThat(type).isEqualTo(FAKE_TYPE)
        assertThat(values).contains(NI_1, NI_2, NI_3)
    }

    companion object {
        private val NI_1 = NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 100)
        private val NI_2 = NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 2), 200)
        private val NI_3 = NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 3), 300)
        private const val FAKE_TYPE = "FakeType"
    }
}
