package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
class NoOpFinderTest {

    private val noOpFinder: NoOpFinder = NoOpFinder()

    @Test
    fun `should find default podcast`() {
        assertThat(noOpFinder.find("")).isEqualTo(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(noOpFinder.compatibility("")).isEqualTo(-1)
    }
}
