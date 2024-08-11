package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.storage.CoverExistsRequest
import com.github.davinkevin.podcastserver.service.storage.DeleteRequest
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID.randomUUID
import kotlin.io.path.Path

@ExtendWith(SpringExtension::class)
@Import(CoverService::class)
@Suppress("UnassignedFluxMonoInstance")
class CoverServiceTest (
    @Autowired val service: CoverService
) {
    private val date = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @MockBean private lateinit var cover: CoverRepository
    @MockBean private lateinit var file: FileStorageService

    @Nested
    @DisplayName("should delete old covers")
    inner class ShouldDeleteOldCovers {

        @AfterEach
        fun afterEach() = Mockito.reset(cover, file)

        @Test
        fun `with no cover to delete`() {
            /* Given */
            whenever(cover.findCoverOlderThan(date)).thenReturn(emptyList())
            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            verify(cover).findCoverOlderThan(date)
            verify(file, never()).delete(any<DeleteRequest.ForCover>())
        }

        @Test
        fun `with covers existing`() {
            /* Given */
            val covers = listOf(
                randomCover("item1", "podcast1"),
                randomCover("item2", "podcast2"),
                randomCover("item3", "podcast3")
            )
            whenever(cover.findCoverOlderThan(date)).thenReturn(covers)
            whenever(file.coverExists(any<CoverExistsRequest.ForItem>())).thenReturn(Path("file.mp3"))
            whenever(file.delete(any<DeleteRequest.ForCover>())).thenReturn(true)

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            verify(file, times(3)).delete(any<DeleteRequest.ForCover>())
        }

        @Test
        fun `with cover not existing`() {
            /* Given */
            val covers = listOf(randomCover("item1", "podcast1"))

            whenever(cover.findCoverOlderThan(date)).thenReturn(covers)
            whenever(file.coverExists(any<CoverExistsRequest.ForItem>())).thenReturn(null)

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            verify(file, never()).delete(any<DeleteRequest.ForCover>())
        }

    }

}

private fun randomCover(itemTitle: String, podcastTitle: String) =
    DeleteRequest.ForCover(
        id = randomUUID(),
        extension = "png",
        item = DeleteRequest.ForCover.Item(randomUUID(), itemTitle),
        podcast = DeleteRequest.ForCover.Podcast(randomUUID(), podcastTitle)
    )
