package com.github.davinkevin.podcastserver.utils.form

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Function

internal class MovingItemInQueueFormTest {

    val id = Function<MovingItemInQueueForm, Any> { it.id }
    val position = Function<MovingItemInQueueForm, Any> { it.position }

    @Test
    fun `should instantiate and define fields`() {
        /* Given */
        val uuid = UUID.randomUUID()
        /* When */
        val form = MovingItemInQueueForm(id = uuid, position = 5)
        /* Then */
        assertThat(form)
                .extracting(id, position)
                .containsExactly(uuid, 5)
    }

    @Test
    fun `should be equal and has same hashcode`() {
        /* Given */
        val id = UUID.randomUUID()
        /* When */
        val form1 = MovingItemInQueueForm(id = id, position = 5)
        val form2 = MovingItemInQueueForm(id = id, position = 5)
        /* Then */
        assertThat(form1).isEqualTo(form2)
        assertThat(setOf(form1, form2)).hasSize(1)
    }
}

