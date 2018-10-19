package com.github.davinkevin.podcastserver.manager.worker.noop

import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Updater
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
                .isEqualTo(Updater.NO_MODIFICATION_TUPLE)
    }

    @Test
    fun `should return an empty set of items`() {
        assertThat(noOpUpdater.getItems(Podcast()))
                .isEmpty()
    }

    @Test
    fun `should return an empty signature`() {
        assertThat(noOpUpdater.signatureOf(Podcast())).isEmpty()
    }

    @Test
    fun `should return an everytime false predicate`() {
        assertThat(noOpUpdater.notIn(Podcast()).test(Item())).isFalse()
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
