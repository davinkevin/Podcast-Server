package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.Item
import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.Podcast
import com.github.davinkevin.podcastserver.service.FileService
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.test.StepVerifier
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID.randomUUID

@ExtendWith(SpringExtension::class)
@Import(CoverService::class)
@Suppress("UnassignedFluxMonoInstance")
class CoverServiceTest (
    @Autowired val service: CoverService
) {
    private val date = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @MockBean private lateinit var cover: CoverRepository
    @MockBean private lateinit var file: FileService

    @Nested
    @DisplayName("should delete old covers")
    inner class ShouldDeleteOldCovers {

        @AfterEach fun afterEach() = Mockito.reset(cover, file)

        @Test
        fun `with no cover to delete`(): Unit = runBlocking {
            /* Given */
            whenever(cover.findCoverOlderThan(date)).thenReturn(emptyFlow())
            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            verify(cover, times(1)).findCoverOlderThan(date)
            verify(file, never()).deleteCover(any())
        }

        @Test
        fun `with covers existing`(): Unit = runBlocking {
            /* Given */
            val covers = listOf(
                    randomCover("item1", "podcast1"),
                    randomCover("item2", "podcast2"),
                    randomCover("item3", "podcast3")
            )
            whenever(cover.findCoverOlderThan(date)).thenReturn(covers.asFlow())
            whenever(file.coverExists(any(), any(), any())).thenReturn(Mono.just(""))
            whenever(file.deleteCover(any())).thenReturn(Mono.empty())

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            verify(file, times(3)).deleteCover(any())
        }

        @Test
        fun `with cover not existing`(): Unit = runBlocking {
            /* Given */
            val covers = listOf(randomCover("item1", "podcast1"))

            whenever(cover.findCoverOlderThan(date)).thenReturn(covers.asFlow())
            whenever(file.coverExists(any(), any(), any())).thenReturn(Mono.empty())

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)


            /* Then */
            verify(file, never()).deleteCover(any())
        }

    }

}

private fun randomCover(itemTitle: String, podcastTitle: String) =
        DeleteCoverInformation(randomUUID(), "png", Item(randomUUID(), itemTitle), Podcast(randomUUID(), podcastTitle))
