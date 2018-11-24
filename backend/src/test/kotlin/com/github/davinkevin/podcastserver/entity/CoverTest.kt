package com.github.davinkevin.podcastserver.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*


/**
 * Created by kevin on 14/06/15 for HackerRank problem
 */
class CoverTest {

    @Test
    fun should_create_a_cover_with_url() {
        val cover = Cover().apply { url = NOWHERE_URL }

        cover.apply {
            assertThat(url).isEqualTo(NOWHERE_URL)
            assertThat(id).isNull()
            assertThat(height == null).isTrue()
            assertThat(width == null).isTrue()
        }
    }

    @Test
    fun should_create_a_cover_with_all_parameters() {
        val cover = Cover().apply {
            url = NOWHERE_URL
            width = 100
            height = 200
        }

        cover.apply {
            assertThat(url).isEqualTo(NOWHERE_URL)
            assertThat(id).isNull()
            assertThat(width).isEqualTo(100)
            assertThat(height).isEqualTo(200)
        }
    }

    @Test
    fun should_interact_with_id() {
        /* Given */
        val cover = Cover()
        val id = UUID.randomUUID()

        /* When */
        cover.id = id

        /* Then */
        assertThat(cover.id).isEqualTo(id)
    }

    @Test
    fun should_be_equals_and_has_same_hashcode() {
        /* Given */
        val cover = Cover().apply { url = NOWHERE_URL }
        val coverWithSameUrl = Cover().apply { url = NOWHERE_URL }
        val notCover = Any()
        val coverWithDiffUrl = Cover().apply { url = "NotNowhereUrl" }

        /* Then */
        assertThat(cover).isEqualTo(cover)
        assertThat(cover).isEqualTo(coverWithSameUrl)
        assertThat(cover).isNotEqualTo(notCover)
        assertThat(cover).isNotEqualTo(coverWithDiffUrl)
        assertThat(cover.hashCode()).isEqualTo(coverWithSameUrl.hashCode())
    }

    companion object {
        private const val NOWHERE_URL = "http://nowhere.com"
    }
}
