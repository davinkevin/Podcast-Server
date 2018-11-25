package com.github.davinkevin.podcastserver.manager.worker.noop

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by kevin on 17/03/2016 for Podcast Server
 */
class NoOpUpdaterTest {

    private val noOpUpdater = NoOpUpdater()

    @Test
    fun `should return no modification tuple`() {
        assertThat(noOpUpdater.update(Podcast()))
                .isEqualTo(Updater.NO_MODIFICATION)
    }

    @Test
    fun `should return an empty set of items`() {
        assertThat(noOpUpdater.findItems(Podcast()))
                .isEmpty()
    }

    @Test
    fun `should return an empty signature`() {
        assertThat(noOpUpdater.signatureOf(Podcast())).isEmpty()
    }

    @Test
    fun `should return an everytime false predicate`() {
        assertThat(noOpUpdater.notIn(Podcast())(Item())).isFalse()
    }

    @Test
    fun `should return its type`() {
        assertThat(noOpUpdater.type().key).isEqualTo("NoOpUpdater")
        assertThat(noOpUpdater.type().name).isEqualTo("NoOpUpdater")
    }

    @Test
    fun `should not be compatible`() {
        assertThat(noOpUpdater.compatibility("foo"))
                .isEqualTo(Integer.MAX_VALUE)
    }
}
