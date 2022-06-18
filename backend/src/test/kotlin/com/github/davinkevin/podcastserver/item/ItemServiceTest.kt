package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.FINISH
import com.github.davinkevin.podcastserver.entity.Status.NOT_DOWNLOADED
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileMetaData
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.nio.file.Paths
import java.time.*
import java.util.*
import kotlin.io.path.Path

/**
 * Created by kevin on 2019-02-12
 */
@ExtendWith(SpringExtension::class)
@Import(ItemService::class)
@Suppress("UnassignedFluxMonoInstance")
class ItemServiceTest(
        @Autowired val itemService: ItemService,
        @Autowired val clock: Clock
) {

    @MockBean private lateinit var repository: ItemRepository
    @MockBean private lateinit var p: PodcastServerParameters
    @MockBean private lateinit var fileService: FileStorageService
    @MockBean private lateinit var idm: ItemDownloadManager
    @MockBean private lateinit var podcastRepository: PodcastRepository

    val item = Item(
            id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
            title = "Foo",
            url = "https://external.domain.tld/foo/bar.mp4",

            pubDate = OffsetDateTime.now(),
            downloadDate = OffsetDateTime.now(),
            creationDate = OffsetDateTime.now(),

            description = "desc",
            mimeType = "audio/mp3",
            length = 100,
            fileName = null,
            status = NOT_DOWNLOADED,

            podcast = Item.Podcast(
                    id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                    title = "Podcast Bar",
                    url = "https://external.domain.tld/bar.rss"
            ),
            cover = Item.Cover(
                    id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                    url = URI("https://external.domain.tld/foo/bar.png"),
                    width = 200,
                    height = 200
            )
    )


    @Test
    fun `should delete old items`() {
        /* Given */
        val limit = OffsetDateTime.now().minusDays(30)
        val items = listOf(
                DeleteItemRequest(UUID.fromString("2e7d6cc7-c3ed-47d1-866f-7f797624124d"), Path("foo"), "bar"),
                DeleteItemRequest(UUID.fromString("dca41d0b-a59c-43fa-8d2d-2129fb637546"), Path("num1"), "num2"),
                DeleteItemRequest(UUID.fromString("40430ce3-b421-4c82-b34d-2deb4c46b1cd"), Path("itemT"), "podcastT")
        )
        val repoResponse = Flux.fromIterable(items)
        whenever(repository.findAllToDelete(limit)).thenReturn(repoResponse)
        whenever(fileService.
        deleteItem(any())).thenReturn(Mono.empty())
        whenever(repository.updateAsDeleted(any())).thenReturn(Mono.empty())

        /* When */
        StepVerifier.create(itemService.deleteItemOlderThan(limit))
                .expectSubscription()
                .then {
                    val ids = items.map { it.id }

                    verify(repository).findAllToDelete(limit)
                    verify(fileService, times(3)).deleteItem(argWhere { it in items })
                    verify(repository).updateAsDeleted(argWhere { it == ids })
                }
                /* Then */
                .verifyComplete()
    }

    @Test
    fun `should find by id`() {
        /* Given */
        whenever(repository.findById(any<UUID>())).thenReturn(item.toMono())
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
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(true.toMono())
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(repository, never()).hasToBeDeleted(any())
            verify(repository, never()).findById(any<UUID>())
            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and do nothing because the podcast is delete protected`() {
            /* Given */
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false.toMono())
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(repository.hasToBeDeleted(item.id)).thenReturn(false.toMono())

            /* When */
            StepVerifier.create(itemService.reset(item.id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(item)
                    .verifyComplete()

            verify(repository, never()).findById(any<UUID>())
            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `and do nothing because element is not downloaded`() {
            /* Given */
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false.toMono())
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
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false.toMono())
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
            val currentItem = item.copy(status = FINISH, fileName = Path(""))
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false.toMono())
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
            val currentItem = item.copy(status = FINISH, fileName = Path("foo.mp4"))
            val deleteItemInformation = DeleteItemRequest(currentItem.id, currentItem.fileName!!, currentItem.podcast.title)
            whenever(repository.resetById(item.id)).thenReturn(item.toMono())
            whenever(idm.isInDownloadingQueueById(item.id)).thenReturn(false.toMono())
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

    @Nested
    @DisplayName("should find all playlists containing an item by id")
    inner class ShouldFindAllPlaylistsContainingAnItemById {

        @Test
        fun `and return nothing because no playlist contains this item`() {
            /* Given */
            val uuid = UUID.randomUUID()
            whenever(repository.findPlaylistsContainingItem(uuid)).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(itemService.findPlaylistsContainingItem(uuid))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

        }

        @Test
        fun `and return 3 playlist associated to this item`() {
            /* Given */
            val uuid = UUID.randomUUID()
            whenever(repository.findPlaylistsContainingItem(uuid)).thenReturn(Flux.just(
                    ItemPlaylist(UUID.fromString("50958264-d5ed-4a9a-a875-5173bb207720"), "foo"),
                    ItemPlaylist(UUID.fromString("e053b63c-dc1d-4a3a-9c95-8f616a74d2aa"), "bar"),
                    ItemPlaylist(UUID.fromString("6761208b-85e7-4098-817a-2db7c4de7ceb"), "other")
            ))

            /* When */
            StepVerifier.create(itemService.findPlaylistsContainingItem(uuid))
                    /* Then */
                    .expectSubscription()
                    .expectNext(ItemPlaylist(UUID.fromString("50958264-d5ed-4a9a-a875-5173bb207720"), "foo"))
                    .expectNext(ItemPlaylist(UUID.fromString("e053b63c-dc1d-4a3a-9c95-8f616a74d2aa"), "bar"))
                    .expectNext(ItemPlaylist(UUID.fromString("6761208b-85e7-4098-817a-2db7c4de7ceb"), "other"))
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should delete by id")
    inner class ShouldDeleteById {

        @BeforeEach
        fun beforeEach() = Mockito.reset(fileService, repository)

        @Test
        fun `an item which should not be deleted from disk`() {
            /* Given */
            val id = UUID.randomUUID()
            whenever(repository.deleteById(id)).thenReturn(Mono.empty())
            whenever(idm.removeItemFromQueueAndDownload(id)).thenReturn(Mono.empty())
            /* When */
            StepVerifier.create(itemService.deleteById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            verify(fileService, never()).deleteItem(any())
        }

        @Test
        fun `an item which should be deleted from disk`() {
            /* Given */
            val id = UUID.randomUUID()
            val deleteItem = DeleteItemRequest(id, Path("foo"), "bar")
            whenever(repository.deleteById(id)).thenReturn(Mono.just(deleteItem))
            whenever(fileService.deleteItem(deleteItem)).thenReturn(Mono.empty())
            whenever(idm.removeItemFromQueueAndDownload(id)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(itemService.deleteById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            verify(fileService, times(1)).deleteItem(deleteItem)
        }
    }

    @Nested
    @DisplayName("should update file")
    inner class ShouldUploadFile {

        @Test
        fun `with success`() {
            /* Given */
            val file: FilePart = mock()
            val podcast = Podcast(
                    id = UUID.fromString("2cdae1af-f93f-47f2-9a09-a316f2732fc1"),
                    title = "podcast",
                    description = "desc",
                    signature = "sign",
                    url = null,
                    hasToBeDeleted = true,
                    lastUpdate = OffsetDateTime.now(),
                    type = "RSS",
                    tags = emptyList(),
                    cover = Cover(
                            id = UUID.fromString("e63f4c96-ce26-485a-bb8a-c3e799f843dd"),
                            url = URI("17aaa117-afe7-4165-9468-fafb61d13bdb"),
                            height = 100,
                            width = 100
                    )
            )

            val itemToCreate = ItemForCreation(
                    title = "title",
                    url = null,

                    pubDate = ZonedDateTime.of(LocalDateTime.of(2020, 1, 2, 0, 0), ZoneId.systemDefault())
                            .toOffsetDateTime(),
                    downloadDate = OffsetDateTime.now(clock),
                    creationDate = OffsetDateTime.now(clock),

                    description = podcast.description,
                    mimeType = "audio/mp3",
                    length = 1234L,
                    fileName = Path("Podcast_Name_-_2020-01-02_-_title.mp3"),
                    status = FINISH,

                    podcastId = podcast.id,
                    cover = CoverForCreation(100, 100, podcast.cover.url)
            )
            val itemCreated = Item(
                    id = UUID.fromString("a9f303e9-e53c-450c-97b6-7f16e8b2f541"),
                    title = itemToCreate.title,
                    url = null,
                    pubDate = itemToCreate.pubDate,
                    downloadDate = itemToCreate.downloadDate,
                    creationDate = itemToCreate.creationDate,
                    description = itemToCreate.description,
                    mimeType = itemToCreate.mimeType,
                    length = itemToCreate.length,
                    status = FINISH,
                    fileName = itemToCreate.fileName,
                    podcast = Item.Podcast(podcast.id, podcast.title, podcast.url),
                    cover = Item.Cover(
                            id = UUID.fromString("f60ece68-d95b-4990-947d-bfe70ecb135c"),
                            url = podcast.cover.url,
                            width = podcast.cover.width,
                            height = podcast.cover.height
                    )
            )
            val fileName = "Podcast Name - 2020-01-02 - title.mp3"
            val normalizedFileName = Paths.get(fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_"))
            whenever(file.filename()).thenReturn(fileName)
            whenever(podcastRepository.findById(podcast.id)).thenReturn(podcast.toMono())
            whenever(fileService.cache(file, normalizedFileName)).thenReturn(normalizedFileName.toMono())
            whenever(fileService.upload(podcast.title, normalizedFileName)).thenReturn(Mono.empty())
            whenever(fileService.metadata(podcast.title, normalizedFileName)).thenReturn(
                FileMetaData("audio/mp3", 1234L).toMono()
            )
            whenever(repository.create(itemToCreate)).thenReturn(itemCreated.toMono())
            whenever(podcastRepository.updateLastUpdate(podcast.id)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(itemService.upload(podcast.id, file))
                    /* Then */
                    .expectSubscription()
                    .expectNext(itemCreated)
                    .verifyComplete()
        }
    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun fixedClock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
    }
}

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)
