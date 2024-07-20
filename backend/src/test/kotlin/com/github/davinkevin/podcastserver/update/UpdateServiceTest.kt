package com.github.davinkevin.podcastserver.update

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.ItemForCreation
import com.github.davinkevin.podcastserver.item.ItemRepository
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.awaitility.Awaitility.await
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
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import java.net.URI
import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit

private val fixedDate = Clock.fixed(OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

@ExtendWith(SpringExtension::class)
class UpdateServiceTest(
    @Autowired private val service: UpdateService
) {
    abstract class FakeUpdater : Updater
    @MockBean lateinit var podcastRepository: PodcastRepository
    @MockBean lateinit var itemRepository: ItemRepository
    @MockBean lateinit var updaters: UpdaterSelector
    @MockBean lateinit var liveUpdate: MessagingTemplate
    @MockBean lateinit var fileService: FileStorageService
    @MockBean lateinit var idm: ItemDownloadManager

    @TestConfiguration
    @Import(UpdateService::class)
    class LocalTestConfig {
        @Bean fun updateExecutor(): SimpleAsyncTaskExecutor = SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())
    }

    @BeforeEach
    fun beforeEach() {
        Mockito.reset(
            podcastRepository,
            itemRepository,
            updaters,
            liveUpdate,
            fileService,
            idm
        )
    }

    @Nested
    @DisplayName("should manage individual update")
    inner class ShouldManageIndividualUpdate {

        private val podcast = Podcast(
            id = UUID.fromString("50167bbf-7a6f-4876-bc2c-93449d523d39"),
            title = "podcast",
            description = "desc",
            signature = "sign",
            url = "https://localhost:5555/rss.xml",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.now(fixedDate),
            type = "RSS",
            tags = emptyList(),
            cover = Cover(
                id = UUID.fromString("e63f4c96-ce26-485a-bb8a-c3e799f843dd"),
                url = URI("17aaa117-afe7-4165-9468-fafb61d13bdb"),
                height = 100,
                width = 100
            )
        )

        private val items = listOf(
            ItemFromUpdate(
                title = "title",
                pubDate = ZonedDateTime.now(fixedDate),
                length = 1234,
                mimeType = "audio/mp3",
                url = URI("http://localhost:1234/item/1"),
                description = "desc",
                cover = null
            ),
            ItemFromUpdate(
                title = "title",
                pubDate = null,
                length = 1234,
                mimeType = "audio/mp3",
                url = URI("http://localhost:1234/item/2"),
                description = null,
                cover = ItemFromUpdate.Cover(100, 100, URI("http://localhost:1234/item/2.png"))
            )
        )

        @Test
        fun `and do nothing because the podcast has no url`() {
            /* Given */
            val p = podcast.copy(
                url = null
            )
            whenever(podcastRepository.findById(podcast.id)).thenReturn(p)

            /* When */
            service.update(p.id)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                verify(podcastRepository).findById(p.id)
                verifyNoMoreInteractions(
                    podcastRepository,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `on a podcast without any item to force signature reset`() {
            /* Given */
            whenever(podcastRepository.findById(podcast.id)).thenReturn(podcast)
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn emptyList()
                on { update(any()) }.thenCallRealMethod()
            }
            val uri = URI(podcast.url!!)
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast.id), any())

            /* When */
            service.update(podcast.id)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                verify(podcastRepository).findById(podcast.id)
                verify(podcastRepository).updateSignature(podcast.id, "")
                verify(updaters).of(uri)
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `on a podcast with 2 new items`() {
            /* Given */
            whenever(podcastRepository.findById(podcast.id)).thenReturn(podcast)
            val uri = URI(podcast.url!!)
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).then { args ->
                args.getArgument<List<ItemForCreation>>(0)
                    .map { it.toItem(podcast) }
            }
            doNothing().whenever(fileService).downloadItemCover(any())
            doNothing().whenever(podcastRepository).updateLastUpdate(eq(podcast.id))

            /* When */
            service.update(podcast.id)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findById(podcast.id)
                    verify().updateSignature(podcast.id, "another-signature")
                    verify().updateLastUpdate(podcast.id)
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verify(fileService, times(2)).downloadItemCover(any())
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `on a podcast with 1 new item but with a problematic cover`() {
            /* Given */
            whenever(podcastRepository.findById(podcast.id)).thenReturn(podcast)
            val uri = URI(podcast.url!!)
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).then { args ->
                args.getArgument<List<ItemForCreation>>(0)
                    .map { it.toItem(podcast) }
            }
            doNothing().whenever(fileService).downloadItemCover(any())
            doNothing().whenever(podcastRepository).updateLastUpdate(eq(podcast.id))

            /* When */
            service.update(podcast.id)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findById(podcast.id)
                    verify().updateSignature(podcast.id, "another-signature")
                    verify().updateLastUpdate(podcast.id)
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verify(fileService, times(2)).downloadItemCover(any())
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `on a podcast with 1 already existing item `() {
            /* Given */
            whenever(podcastRepository.findById(podcast.id)).thenReturn(podcast)
            val uri = URI(podcast.url!!)
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).thenReturn(emptyList())

            /* When */
            service.update(podcast.id)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findById(podcast.id)
                    verify().updateSignature(podcast.id, "another-signature")
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }


        @Test
        fun `on a podcast with an updater returning no data after the update`() {
            /* Given */
            val p = podcast.copy(signature = "a specific signature")
            whenever(podcastRepository.findById(podcast.id)).thenReturn(p)
            val uri = URI(p.url!!)
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) }.then { error("error during signature check") }
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)

            /* When */
            service.update(p.id)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    return@inOrder ""
                }
                verify(podcastRepository).findById(p.id)
                verify(updaters).of(uri)
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

    }

    @Nested
    @DisplayName("should manage multiple updates")
    inner class ShouldManageMultipleUpdates {

        private val podcast1 = Podcast(
            id = UUID.fromString("50167bbf-7a6f-4876-bc2c-93449d523d39"),
            title = "podcast",
            description = "desc",
            signature = "sign",
            url = "https://localhost:5555/rss.xml",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.now(fixedDate),
            type = "RSS",
            tags = emptyList(),
            cover = Cover(
                id = UUID.fromString("e63f4c96-ce26-485a-bb8a-c3e799f843dd"),
                url = URI("17aaa117-afe7-4165-9468-fafb61d13bdb"),
                height = 100,
                width = 100
            )
        )

        private val podcast2 = Podcast(
            id = UUID.fromString("8bdbc1e6-4b87-4dab-9900-82d7349ac4dc"),
            title = "podcast",
            description = "desc",
            signature = "sign",
            url = null,
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.now(fixedDate),
            type = "RSS",
            tags = emptyList(),
            cover = Cover(
                id = UUID.fromString("e63f4c96-ce26-485a-bb8a-c3e799f843dd"),
                url = URI("17aaa117-afe7-4165-9468-fafb61d13bdb"),
                height = 100,
                width = 100
            )
        )


        private val items = listOf(
            ItemFromUpdate(
                title = "title",
                pubDate = ZonedDateTime.now(fixedDate),
                length = 1234,
                mimeType = "audio/mp3",
                url = URI("http://localhost:1234/item/1"),
                description = "desc",
                cover = ItemFromUpdate.Cover(100, 100, URI("http://localhost:1234/item/1.png"))
            ),
            ItemFromUpdate(
                title = "title",
                pubDate = ZonedDateTime.now(fixedDate),
                length = 1234,
                mimeType = "audio/mp3",
                url = URI("http://localhost:1234/item/2"),
                description = "desc",
                cover = ItemFromUpdate.Cover(100, 100, URI("http://localhost:1234/item/2.png"))
            )
        )

        @Test
        fun `with a result with 2 items for the only podcast able to be updated`() {
            /* Given */
            whenever(podcastRepository.findAll()).thenReturn(listOf(podcast1, podcast2))
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            val uri = URI(podcast1.url!!)
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast1.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).then { args ->
                args.getArgument<List<ItemForCreation>>(0)
                    .map { it.toItem(podcast1) }
            }
            doNothing().whenever(fileService).downloadItemCover(any())
            doNothing().whenever(podcastRepository).updateLastUpdate(podcast1.id)

            /* When */
            service.updateAll(force = false, download = false)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findAll()
                    verify().updateSignature(podcast1.id, "another-signature")
                    verify().updateLastUpdate(podcast1.id)
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verify(fileService, times(2)).downloadItemCover(any())
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `with a result with 2 items and download mode activated`() {
            /* Given */
            whenever(podcastRepository.findAll()).thenReturn(listOf(podcast1, podcast2))
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            val uri = URI(podcast1.url!!)
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast1.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).then { args ->
                args.getArgument<List<ItemForCreation>>(0)
                    .map { it.toItem(podcast1) }
            }
            doNothing().whenever(fileService).downloadItemCover(any())
            doNothing().whenever(podcastRepository).updateLastUpdate(podcast1.id)
            doNothing().whenever(idm).launchDownload()

            /* When */
            service.updateAll(force = false, download = true)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findAll()
                    verify().updateSignature(podcast1.id, "another-signature")
                    verify().updateLastUpdate(podcast1.id)
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verify(fileService, times(2)).downloadItemCover(any())
                verify(idm).launchDownload()
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `with a result with 2 items and force mode activated`() {
            /* Given */
            whenever(podcastRepository.findAll()).thenReturn(listOf(podcast1, podcast2))
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            val uri = URI(podcast1.url!!)
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(podcast1.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).then { args ->
                args.getArgument<List<ItemForCreation>>(0)
                    .map { it.toItem(podcast1) }
            }
            doNothing().whenever(fileService).downloadItemCover(any())
            doNothing().whenever(podcastRepository).updateLastUpdate(podcast1.id)

            /* When */
            service.updateAll(force = true, download = false)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findAll()
                    verify().updateSignature(podcast1.id, "another-signature")
                    verify().updateLastUpdate(podcast1.id)
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verify(fileService, times(2)).downloadItemCover(any())
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }

        @Test
        fun `with a podcast with a signature null due to previous error`() {
            /* Given */
            val p = podcast1.copy(signature = null)
            whenever(podcastRepository.findAll()).thenReturn(listOf(p))
            val fakeUpdater = mock<FakeUpdater> {
                on { signatureOf(any()) } doReturn "another-signature"
                on { findItems(any()) } doReturn items
                on { update(any()) }.thenCallRealMethod()
            }
            val uri = URI(p.url!!)
            whenever(updaters.of(uri)).thenReturn(fakeUpdater)
            doNothing().whenever(podcastRepository).updateSignature(eq(p.id), any())
            whenever(itemRepository.create(any<List<ItemForCreation>>())).then { args ->
                args.getArgument<List<ItemForCreation>>(0)
                    .map { it.toItem(p) }
            }
            doNothing().whenever(fileService).downloadItemCover(any())
            doNothing().whenever(podcastRepository).updateLastUpdate(p.id)

            /* When */
            service.updateAll(force = false, download = false)

            /* Then */
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                liveUpdate.inOrder {
                    verify().isUpdating(true)
                    verify().isUpdating(false)
                }
                fakeUpdater.inOrder {
                    verify().update(any())
                    verify().signatureOf(any())
                    verify().findItems(any())
                }
                podcastRepository.inOrder {
                    verify().findAll()
                    verify().updateSignature(p.id, "another-signature")
                    verify().updateLastUpdate(p.id)
                }
                verify(updaters).of(uri)
                verify(itemRepository).create(any<List<ItemForCreation>>())
                verify(fileService, times(2)).downloadItemCover(any())
                verifyNoMoreInteractions(
                    podcastRepository,
                    fakeUpdater,
                    itemRepository,
                    updaters,
                    liveUpdate,
                    fileService,
                    idm
                )
            }
        }
    }
}

private fun ItemForCreation.toItem(p: Podcast) = Item(
    id = UUID.randomUUID(),
    title = title,
    url = url,
    pubDate = pubDate,
    downloadDate = null,
    creationDate = null,
    description = description,
    mimeType = mimeType,
    length = length,
    fileName = fileName,
    status = Status.NOT_DOWNLOADED,
    podcast = Item.Podcast(
        id = p.id,
        title = p.title,
        url = p.url,
    ),
    cover = Item.Cover(
        id = UUID.randomUUID(),
        url = cover?.url ?: p.cover.url,
        width = cover?.width ?: p.cover.width,
        height = cover?.height ?: p.cover.height,
    )
)