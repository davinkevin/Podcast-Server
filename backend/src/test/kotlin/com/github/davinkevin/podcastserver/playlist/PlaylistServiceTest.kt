package com.github.davinkevin.podcastserver.playlist

import org.mockito.kotlin.whenever
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
 * Created by kevin on 2019-07-06
 */
@ExtendWith(SpringExtension::class)
@Import(PlaylistService::class)
class PlaylistServiceTest(
    @Autowired val service: PlaylistService
) {

    @MockBean private lateinit var repository: PlaylistRepository

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

    @Nested
    @DisplayName("should find by Id")
    inner class ShouldFindById {

        @Test
        fun `with no playlist`() {
            /* Given */
            val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            whenever(repository.findById(id)).thenReturn(Mono.empty())
            /* When */
            StepVerifier.create(service.findById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with one playlist`() {
            /* Given */
            val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            val playlist = PlaylistWithItems(id = id, name = "foo", items = emptyList())
            whenever(repository.findById(id)).thenReturn(playlist.toMono())

            /* When */
            StepVerifier.create(service.findById(id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlist)
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should save")
    inner class ShouldSave {

        @Test
        fun `with a name`() {
            /* Given */
            val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            val playlist = PlaylistWithItems(id = id, name = "foo", items = emptyList())
            whenever(repository.save("foo")).thenReturn(playlist.toMono())

            /* When */
            StepVerifier.create(repository.save("foo"))
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlist)
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should add")
    inner class ShouldAdd {

        @Test
        fun `to playlist`() {
            /* Given */
            val playlistId = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            val itemId = UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            val playlist = PlaylistWithItems(id = playlistId, name = "foo", items = emptyList())
            whenever(repository.addToPlaylist(playlistId, itemId)).thenReturn(playlist.toMono())
            /* When */
            StepVerifier.create(service.addToPlaylist(playlistId, itemId))
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlist)
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should remove")
    inner class ShouldRemove {

        @Test
        fun `from playlist`() {
            /* Given */
            val playlistId = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            val itemId = UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            val playlist = PlaylistWithItems(id = playlistId, name = "foo", items = emptyList())
            whenever(repository.removeFromPlaylist(playlistId, itemId)).thenReturn(playlist.toMono())
            /* When */
            StepVerifier.create(service.addToPlaylist(playlistId, itemId))
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlist)
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {
        @Test
        fun `by id`() {
            /* Given */
            val id = UUID.randomUUID()
            whenever(repository.deleteById(id)).thenReturn(Mono.empty())
            /* When */
            StepVerifier.create(repository.deleteById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }
    }
}
