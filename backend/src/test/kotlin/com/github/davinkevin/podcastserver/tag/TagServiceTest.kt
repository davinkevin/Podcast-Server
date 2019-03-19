package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.util.*
import com.github.davinkevin.podcastserver.tag.TagRepositoryV2 as TagRepository

/**
 * Created by kevin on 2019-03-24
 */
@ExtendWith(SpringExtension::class)
@Import(TagService::class)
class TagServiceTest {

    @MockBean lateinit var repo: TagRepository
    @Autowired lateinit var service: TagService

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

        @Test
        fun `with existing tag`() {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(repo.findById(id)).thenReturn(Tag(id, "foo").toMono())

            /* When */
            StepVerifier.create(service.findById(id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Tag(id, "foo"))
                    .verifyComplete()
        }

        @Test
        fun `with no result`() {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(repo.findById(id)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(service.findById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }
    }

}
