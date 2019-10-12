package com.github.davinkevin.podcastserver.playlist

import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.*
import com.github.davinkevin.podcastserver.playlist.PlaylistRepositoryV2 as WatchListRepository

/**
 * Created by kevin on 2019-07-06
 */
@ExtendWith(SpringExtension::class)
@Import(PlaylistService::class)
class PlaylistServiceTest {

    @MockBean private lateinit var repository: WatchListRepository
    @Autowired private lateinit var service: PlaylistService

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        @Test
        fun `with no watch list`() {
            /* Given */
            whenever(repository.findAll()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(service.findAll())
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with watch lists in results`() {
            /* Given */
            whenever(repository.findAll()).thenReturn(Flux.just(
                    Playlist(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"),
                    Playlist(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"),
                    Playlist(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third")
            ))
            /* When */
            StepVerifier.create(service.findAll())
                    /* Then */
                    .expectSubscription()
                    .expectNext(Playlist(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"))
                    .expectNext(Playlist(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"))
                    .expectNext(Playlist(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third"))
                    .verifyComplete()
        }


    }
}
