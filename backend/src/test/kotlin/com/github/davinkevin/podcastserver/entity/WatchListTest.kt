package com.github.davinkevin.podcastserver.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*


/**
 * Created by kevin on 19/03/2016 for Podcast Server
 */
class WatchListTest {

    @Test
    fun `should have getters and setters`() {
        /* Given */
        val uuid = "b4156ceb-ddd8-437f-b822-941bf1c14723"
        val items = mutableSetOf<Item>()
        val name = "Foo"

        /* When */
        val watchList = WatchList()
        watchList.id = UUID.fromString(uuid)
        watchList.items = items
        watchList.name = name

        /* Then */
        assertThat(watchList.id).isEqualTo(UUID.fromString(uuid))
        assertThat(watchList.items).isEmpty()
        assertThat(watchList.name).isEqualTo(name)
    }

}
