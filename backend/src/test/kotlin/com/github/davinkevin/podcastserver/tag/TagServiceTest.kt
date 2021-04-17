package com.github.davinkevin.podcastserver.tag

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
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
        fun `with existing tag`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(repo.findById(id)).thenReturn(Tag(id, "foo"))

            /* When */
            val foundTag = service.findById(id)

            /* Then */
            assertThat(foundTag).isEqualTo(Tag(id, "foo"))
        }

        @Test
        fun `with no result`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(repo.findById(id)).thenReturn(null)

            /* When */
            val foundTag = service.findById(id)

            /* Then */
            assertThat(foundTag).isNull()
        }
    }

    @Nested
    @DisplayName("should find by name")
    inner class ShouldFindByName {

        @Test
        fun `with existing tags`(): Unit = runBlocking {
            /* Given */
            val tag = Tag(UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf"), "foo")
            whenever(repo.findByNameLike("foo")).thenReturn(flowOf(tag))

            /* When */
            val foundTags = service.findByNameLike("foo").toList()

            /* Then */
            assertThat(foundTags).containsOnly(tag)
        }

        @Test
        fun `with no result`(): Unit = runBlocking {
            /* Given */
            whenever(repo.findByNameLike("foo")).thenReturn(emptyFlow())

            /* When */
            val foundTags = service.findByNameLike("foo").toList()

            /* Then */
            assertThat(foundTags).isEmpty()
        }

    }
}
