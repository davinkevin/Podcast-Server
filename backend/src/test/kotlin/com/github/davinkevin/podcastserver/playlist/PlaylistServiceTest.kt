package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.defaultCoverInformation
import com.github.davinkevin.podcastserver.service.image.toCoverForCreation
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
import java.net.URI
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
    @MockBean private lateinit var image: ImageService

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        private val playlist1 = Playlist(
            id = UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"),
            name = "one",
            cover = Playlist.Cover(
                id = UUID.fromString("ee731587-5c14-49ea-b2cb-a1f24b99710c"),
                url = URI.create("https://placeholder.io/600x600"),
                width = 600,
                height = 600,
            )
        )
        private val playlist2 = Playlist(
            id = UUID.fromString("98bcfe02-a1a3-484a-a4a7-24295e84f7ec"),
            name = "two",
            cover = Playlist.Cover(
                id = UUID.fromString("e994a9c7-ad1a-41e3-b8db-b2b40b79dd13"),
                url = URI.create("https://placeholder.io/600x600"),
                width = 600,
                height = 600,
            )
        )
        private val playlist3 = Playlist(
            id = UUID.fromString("7eafc295-bae6-426d-986e-edf03889a682"),
            name = "three",
            cover = Playlist.Cover(
                id = UUID.fromString("11aa9c87-cc2a-49e4-97cc-f3c0f5b59f20"),
                url = URI.create("https://placeholder.io/600x600"),
                width = 600,
                height = 600,
            )
        )

        @Test
        fun `with no playlist`() {
            /* Given */
            whenever(repository.findAll()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(service.findAll())
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with 3 playlists in results`() {
            /* Given */
            whenever(repository.findAll()).thenReturn(Flux.just(playlist1, playlist2, playlist3))
            /* When */
            StepVerifier.create(service.findAll())
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlist1)
                    .expectNext(playlist2)
                    .expectNext(playlist3)
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should find by Id")
    inner class ShouldFindById {

        private val playlist = Playlist(
            id = UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"),
            name = "one",
            cover = Playlist.Cover(
                id = UUID.fromString("ee731587-5c14-49ea-b2cb-a1f24b99710c"),
                url = URI.create("https://placeholder.io/600x600"),
                width = 600,
                height = 600,
            )
        )

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
            whenever(repository.findById(playlist.id))
                .thenReturn(playlist.toMono())

            /* When */
            StepVerifier.create(service.findById(playlist.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlist)
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should save")
    inner class ShouldSave {

        private val id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
        private val playlistToReturn = Playlist(
            id = id,
            name = "foo",
            cover = Playlist.Cover(
                id = UUID.fromString("ee731587-5c14-49ea-b2cb-a1f24b99710c"),
                url = URI.create("https://placeholder.io/600x600"),
                width = 600,
                height = 600,
            )
        )

        private val coverInformation = CoverInformation(
            height = 1600,
            url = URI.create("https://placeholder.io/600x600"),
            width = 1600,
        )

        private val playlistToCreateInDatabase =  PlaylistForDatabaseCreation(
            name = "foo",
            cover = coverInformation.toCoverForCreation()
        )

        private val playlistReceived = PlaylistForCreationV2(
            name = "foo",
            coverUrl = URI.create("https://placeholder.io/600x600")
        )

        @Test
        fun `with existing cover`() {
            /* Given */
            whenever(image.fetchCoverInformation(playlistReceived.coverUrl)).thenReturn(coverInformation.toMono())
            whenever(repository.create(playlistToCreateInDatabase)).thenReturn(playlistToReturn.toMono())

            /* When */
            StepVerifier.create(service.create(playlistReceived))
                    /* Then */
                    .expectSubscription()
                    .expectNext(playlistToReturn)
                    .verifyComplete()
        }

        @Test
        fun `with no cover`() {
            /* Given */
            whenever(image.fetchCoverInformation(playlistReceived.coverUrl))
                .thenReturn(Mono.empty())
            whenever(repository.create(playlistToCreateInDatabase.copy(cover = defaultCoverInformation.toCoverForCreation())))
                .thenReturn(playlistToReturn.toMono())

            /* When */
            StepVerifier.create(service.create(playlistReceived))
                /* Then */
                .expectSubscription()
                .expectNext(playlistToReturn)
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
