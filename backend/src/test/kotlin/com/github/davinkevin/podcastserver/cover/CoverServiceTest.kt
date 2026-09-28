package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
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
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID.randomUUID
import kotlin.io.path.Path

@NestedSpringTest
@ExtendWith(OutputCaptureExtension::class)
@Import(CoverService::class)
@Suppress("UnassignedFluxMonoInstance")
class CoverServiceTest (
    @Autowired val service: CoverService
) {
    private val date = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @MockitoBean private lateinit var cover: CoverRepository
    @MockitoBean private lateinit var file: FileStorageService

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
        fun `with covers existing`(output: CapturedOutput) {
            /* Given */
            val covers = listOf(
                randomCover("item1", "podcast1"),
                randomCover("item2", "podcast2"),
                randomCover("item3", "podcast3")
            )
            whenever(cover.findCoverOlderThan(date)).thenReturn(covers)
            covers.forEach {
                whenever(file.coverExists(CoverExistsRequest.ForItem(it.podcast.title, it.item.id, it.extension)))
                    .thenReturn(Path("${it.item.id}.${it.extension}"))
            }
            whenever(file.delete(any<DeleteRequest.ForCover>())).thenReturn(true)

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            covers.forEach { verify(file).delete(it) }
            assertThat(output.out).contains("INFO").contains("3 candidates, 3 present, 3 deleted, 0 failed")
        }

        @Test
        fun `with a deletion failing`(output: CapturedOutput) {
            /* Given */
            val covers = listOf(
                randomCover("item1", "podcast1"),
                randomCover("item2", "podcast2"),
            )
            whenever(cover.findCoverOlderThan(date)).thenReturn(covers)
            whenever(file.coverExists(any<CoverExistsRequest.ForItem>())).thenReturn(Path("file.png"))
            whenever(file.delete(covers[0])).thenReturn(true)
            whenever(file.delete(covers[1])).thenReturn(false)

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            assertThat(output.out).contains("WARN").contains("2 candidates, 2 present, 1 deleted, 1 failed")
        }

        @Test
        fun `with cover not existing`(output: CapturedOutput) {
            /* Given */
            val covers = listOf(randomCover("item1", "podcast1"))

            whenever(cover.findCoverOlderThan(date)).thenReturn(covers)
            whenever(file.coverExists(any<CoverExistsRequest.ForItem>())).thenReturn(null)

            /* When */
            service.deleteCoversInFileSystemOlderThan(date)

            /* Then */
            verify(file, never()).delete(any<DeleteRequest.ForCover>())
            assertThat(output.out).contains("1 candidates, 0 present, 0 deleted, 0 failed")
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
