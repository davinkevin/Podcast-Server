package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.FINISH
import com.github.davinkevin.podcastserver.entity.Status.NOT_DOWNLOADED
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.podcast.PodcastService
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.BeforeEach
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
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*
import com.github.davinkevin.podcastserver.cover.CoverRepositoryV2 as CoverRepository
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository

/**
 * Created by kevin on 2019-02-12
 */
@ExtendWith(SpringExtension::class)
@Import(ItemService::class)
@Suppress("UnassignedFluxMonoInstance")
class ItemServiceTest {

    @Autowired lateinit var itemService: ItemService
    @MockBean lateinit var repository: ItemRepositoryV2
    @MockBean lateinit var p: PodcastServerParameters
    @MockBean lateinit var fileService: FileService
    @MockBean lateinit var idm: ItemDownloadManager
    @MockBean lateinit var podcastRepository: PodcastRepository
    @MockBean lateinit var coverRepositoryV2: CoverRepository
    @MockBean lateinit var mimeTypeService: MimeTypeService

    val item = Item(
            id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
            title = "Foo",
            url = "https://external.domain.tld/foo/bar.mp4",

            pubDate = OffsetDateTime.now(),
            downloadDate = OffsetDateTime.now(),
            creationDate = OffsetDateTime.now(),

            description = "desc",
            mimeType = null,
            length = 100,
            fileName = null,
            status = NOT_DOWNLOADED,

            podcast = PodcastForItem(
                    id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                    title = "Podcast Bar",
                    url = "https://external.domain.tld/bar.rss"
            ),
            cover = CoverForItem(
                    id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                    url = "https://external.domain.tld/foo/bar.png",
                    width = 200,
                    height = 200
            )
    )


    @Test
    fun `should delete old items`() {
        /* Given */
        val limit = ZonedDateTime.now().minusDays(30)
        whenever(p.limitDownloadDate()).thenReturn(limit)
        val items = listOf(
                DeleteItemInformation(UUID.fromString("2e7d6cc7-c3ed-47d1-866f-7f797624124d"), "foo", "bar"),
                DeleteItemInformation(UUID.fromString("dca41d0b-a59c-43fa-8d2d-2129fb637546"), "num1", "num2"),
                DeleteItemInformation(UUID.fromString("40430ce3-b421-4c82-b34d-2deb4c46b1cd"), "itemT", "podcastT")
        )
        val repoResponse = Flux.fromIterable(items)
        whenever(repository.findAllToDelete(limit.toOffsetDateTime())).thenReturn(repoResponse)
        whenever(fileService.
                deleteItem(any())).thenReturn(Mono.empty())
        whenever(repository.updateAsDeleted(any())).thenReturn(Mono.empty())

        /* When */
        StepVerifier.create(itemService.deleteOldEpisodes())
                .expectSubscription()
                .then {
                    val paths = items.map { it.path }
                    val ids = items.map { it.id }

                    verify(repository).findAllToDelete(limit.toOffsetDateTime())
                    verify(fileService, times(3)).deleteItem(argWhere { it in items })
                    verify(repository).updateAsDeleted(argWhere { it == ids })
                }
                /* Then */
                .verifyComplete()
    }

    @Test
    fun `should find by id`() {
        /* Given */
        whenever(repository.findById(any())).thenReturn(item.toMono())
        /* When */
        StepVerifier.create(itemService.findById(item.id))
                /* Then */
                .expectSubscription()
                .expectNext(item)
                .verifyComplete()
    }

    @Nested
    @DisplayName("should reset")
    inner class ShouldReset {

        @BeforeEach
        fun beforeEach() = Mockito.reset(fileService, repository)

        @Test
        fun `and do nothing because item is currently downloading`() {
            /* Given */
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(true)
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(repository, never()).hasToBeDeleted(any())
            verify(repository, never()).findById(any())
            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and do nothing because the podcast is delete protected`() {
            /* Given */
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false)
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(repository.hasToBeDeleted(item.id)).thenReturn(false.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(repository, never()).findById(any())
            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and do nothing because element is not downloaded`() {
            /* Given */
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false)
            whenever(repository.hasToBeDeleted(item.id)).thenReturn(true.toMono())
            whenever(repository.findById(item.id)).thenReturn(item.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and do nothing because element doesn't have a filename`() {
            /* Given */
            val currentItem = item.copy(status = FINISH, fileName = null)
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false)
            whenever(repository.hasToBeDeleted(item.id)).thenReturn(true.toMono())
            whenever(repository.findById(item.id)).thenReturn(currentItem.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and do nothing because element has filename empty`() {
            /* Given */
            val currentItem = item.copy(status = FINISH, fileName = "")
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false)
            whenever(repository.hasToBeDeleted(item.id)).thenReturn(true.toMono())
            whenever(repository.findById(item.id)).thenReturn(currentItem.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and delete files`() {
            /* Given */
            val currentItem = item.copy(status = FINISH, fileName = "foo.mp4")
            val deleteItemInformation = DeleteItemInformation(currentItem.id, currentItem.fileName!!, currentItem.podcast.title)
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false)
            whenever(repository.hasToBeDeleted(item.id)).thenReturn(true.toMono())
            whenever(repository.findById(item.id)).thenReturn(currentItem.toMono())
            whenever(fileService.deleteItem(deleteItemInformation)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(fileService).deleteItem(deleteItemInformation)
        }



    }

    @Nested
    @DisplayName("shoud search")
    inner class ShouldSearch {

        @Test
        fun `with podcast id`() {
            /* Given */
            val q = ""
            val tags = listOf<String>()
            val statuses = listOf<Status>()
            val page = ItemPageRequest(0, 12, ItemSort("DESC", "title"))
            val podcastId = UUID.fromString("167991ba-44ca-4f2b-b47b-5233a33d33b8")
            val result = PageItem.of(listOf(item), 1, page)
            whenever(repository.search(q, tags, statuses, page, podcastId))
                    .thenReturn(result.toMono())

            /* When */
            StepVerifier.create(itemService.search(q, tags, statuses, page, podcastId))
                    /* Then */
                    .expectSubscription()
                    .expectNext(result)
                    .verifyComplete()
        }

        @Test
        fun `without podcast id`() {
            /* Given */
            val q = ""
            val tags = listOf<String>()
            val statuses = listOf<Status>()
            val page = ItemPageRequest(0, 12, ItemSort("DESC", "title"))
            val result = PageItem.of(listOf(item), 1, page)
            whenever(repository.search(q, tags, statuses, page, null))
                    .thenReturn(result.toMono())

            /* When */
            StepVerifier.create(itemService.search(q, tags, statuses, page))
                    /* Then */
                    .expectSubscription()
                    .expectNext(result)
                    .verifyComplete()
        }
    }
}
