package com.github.davinkevin.podcastserver.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
class TagTest {

    @Test
    fun `should create a tag`() {
        val anId = UUID.randomUUID()
        val tag = Tag().apply {
            name = "Humour"
            id = anId
        }

        assertThat(tag.id).isEqualTo(anId)
        assertThat(tag.name).isEqualTo("Humour")
    }

    @Test
    fun `should be equals`() {
        /* Given */
        val tag = Tag().apply {
            name = "Humour"
            id = UUID.randomUUID()
        }
        val notEquals = Tag().apply {
            name = "Conférence"
            id = UUID.randomUUID()
        }
        val notATag = Any()

        /* When */
        val notSameType = tag == notATag

        /* Then */
        assertThat(tag).isEqualTo(tag)
        assertThat(tag).isNotEqualTo(notEquals)
        assertThat(tag).isNotEqualTo(notSameType)
        assertThat(tag.hashCode()).isEqualTo("Humour".hashCode())
    }
}
