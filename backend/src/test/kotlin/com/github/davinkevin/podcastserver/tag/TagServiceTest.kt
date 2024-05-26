package com.github.davinkevin.podcastserver.tag

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

/**
 * Created by kevin on 2019-03-24
 */
@ExtendWith(SpringExtension::class)
@Import(TagService::class)
class TagServiceTest (
    @Autowired val service: TagService
) {

    @MockBean private lateinit var repo: TagRepository

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

        @Test
        fun `with existing tag`() {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(repo.findById(id)).thenReturn(Tag(id, "foo"))

            /* When */
            val tag = service.findById(id)

            /* Then */
            assertThat(tag).isEqualTo(Tag(id, "foo"))
        }

        @Test
        fun `with no result`() {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(repo.findById(id)).thenReturn(null)

            /* When */
            val tag = service.findById(id)

            /* Then */
            assertThat(tag).isNull()
        }
    }

    @Nested
    @DisplayName("should find by name")
    inner class ShouldFindByName {

        @Test
        fun `with existing tags`() {
            /* Given */
            val tag = Tag(UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf"), "foo")
            whenever(repo.findByNameLike("foo")).thenReturn(listOf(tag))

            /* When */
            val tags = service.findByNameLike("foo")

            /* Then */
            assertThat(tags).contains(tag)
        }

        @Test
        fun `with no result`() {
            /* Given */
            whenever(repo.findByNameLike("foo")).thenReturn(emptyList())

            /* When */
            val tags = service.findByNameLike("foo")

            /* Then */
            assertThat(tags).isEmpty()
        }
    }
}
