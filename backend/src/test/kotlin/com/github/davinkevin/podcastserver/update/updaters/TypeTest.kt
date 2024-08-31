package com.github.davinkevin.podcastserver.update.updaters

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
class TypeTest {

    @Test
    fun should_have_key_and_name() {
        /* Given */
        /* When */
        val type = Type("Key", "Value")
        /* Then */
        assertThat(type.key).isEqualTo("Key")
        assertThat(type.name).isEqualTo("Value")
    }

    @Test
    fun `should be equal if has same key and name`() {
        /* Given */
        val k = "key"
        val n = "name"
        /* When */
        val t1 = Type(k, n)
        val t2 = Type(k, n)
        /* Then */
        assertThat(t1).isEqualTo(t2)
    }

}
