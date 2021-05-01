package com.github.davinkevin.podcastserver.playlist

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
        fun `with no watch list`(): Unit = runBlocking {
            /* Given */
            whenever(repository.findAll()).thenReturn(emptyFlow())
            /* When */
            val playlists = service.findAll().toList()
            /* Then */
            assertThat(playlists).isEmpty()
        }

        @Test
        fun `with watch lists in results`(): Unit = runBlocking {
            /* Given */
            whenever(repository.findAll()).thenReturn(flowOf(
                    Playlist(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"),
                    Playlist(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"),
                    Playlist(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third"),
                )
            )
            /* When */
            val playlists = service.findAll().toList()
            /* Then */
            assertThat(playlists).containsExactly(
                Playlist(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"),
                Playlist(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"),
                Playlist(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third"),
            )
        }
    }

    @Nested
    @DisplayName("should find by Id")
    inner class ShouldFindById {

        @Test
        fun `with no playlist`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            whenever(repository.findById(id)).thenReturn(null)
            /* When */
            val playlist = service.findById(id)
            /* Then */
            assertThat(playlist).isNull()
        }

        @Test
        fun `with one playlist`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            val playlist = PlaylistWithItems(id = id, name = "foo", items = emptyList())
            whenever(repository.findById(id)).thenReturn(playlist)

            /* When */
            val foundPlaylist = service.findById(id)
            /* Then */
            assertThat(foundPlaylist).isEqualTo(playlist)

        }
    }

    @Nested
    @DisplayName("should save")
    inner class ShouldSave {

        @Test
        fun `with a name`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            val playlist = PlaylistWithItems(id = id, name = "foo", items = emptyList())
            whenever(repository.save("foo")).thenReturn(playlist)

            /* When */
            val savedPlaylist = repository.save("foo")

            /* Then */
            assertThat(savedPlaylist).isEqualTo(playlist)
        }

    }

    @Nested
    @DisplayName("should add")
    inner class ShouldAdd {

        @Test
        fun `to playlist`(): Unit = runBlocking {
            /* Given */
            val playlistId = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            val itemId = UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            val playlist = PlaylistWithItems(id = playlistId, name = "foo", items = emptyList())
            whenever(repository.addToPlaylist(playlistId, itemId)).thenReturn(playlist)
            /* When */
            val resultPlaylist = service.addToPlaylist(playlistId, itemId)
            /* Then */
            assertThat(resultPlaylist).isEqualTo(playlist)
        }

    }

    @Nested
    @DisplayName("should remove")
    inner class ShouldRemove {

        @Test
        fun `from playlist`(): Unit = runBlocking {
            /* Given */
            val playlistId = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            val itemId = UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            val playlist = PlaylistWithItems(id = playlistId, name = "foo", items = emptyList())
            whenever(repository.removeFromPlaylist(playlistId, itemId)).thenReturn(playlist)
            /* When */
            val resultPlaylist = service.addToPlaylist(playlistId, itemId)
            /* Then */
            assertThat(resultPlaylist).isEqualTo(playlist)
        }

    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {
        @Test
        fun `by id`(): Unit = runBlocking {
            /* Given */
            val id = UUID.randomUUID()
            /* When */
            repository.deleteById(id)
            /* Then */
            verify(repository, times(1)).deleteById(id)

        }
    }
}
