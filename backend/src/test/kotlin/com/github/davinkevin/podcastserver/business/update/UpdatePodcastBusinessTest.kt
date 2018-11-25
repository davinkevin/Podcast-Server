package com.github.davinkevin.podcastserver.business.update


import arrow.core.Option
import arrow.core.Try
import com.github.davinkevin.podcastserver.business.CoverBusiness
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.manager.worker.UpdatePodcastInformation
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.service.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import javax.validation.ConstraintViolation
import javax.validation.Validator

/**
 * Created by kevin on 09/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class UpdatePodcastBusinessTest {

    @Mock lateinit var podcastRepository: PodcastRepository
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var updaterSelector: UpdaterSelector
    @Mock lateinit var template: MessagingTemplate
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Spy var updateExecutor = ThreadPoolTaskExecutor()
    @Spy var manualExecutor = ThreadPoolTaskExecutor()
    @Mock lateinit var validator: Validator
    @Mock lateinit var coverBusiness: CoverBusiness
    lateinit var updatePodcastBusiness: UpdatePodcastBusiness

    @BeforeEach
    fun beforeEach() {
        updatePodcastBusiness = UpdatePodcastBusiness(podcastRepository, itemRepository, updaterSelector, template, podcastServerParameters, updateExecutor, manualExecutor, validator, coverBusiness)
        updatePodcastBusiness.setTimeOut(1, SECONDS)
        Item.rootFolder = ROOT_FOLDER
        updateExecutor.initialize()
        manualExecutor.initialize()
    }

    @Test
    fun `should delete old episode`() {
        /* Given */
        val p = Podcast().apply { title = "Title" }
        val items = generateItems(3, p).toVΛVΓ()
        val now = ZonedDateTime.now()
        doReturn(items).whenever(itemRepository).findAllToDelete(now)
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(now)

        /* When */
        updatePodcastBusiness.deleteOldEpisode()

        /* Then */
        verify(itemRepository, times(3)).save(argWhere { items.contains(it)  })
        assertThat(items).allMatch { it.status == Status.DELETED && it.fileName == null }
    }

    @Test
    fun `should reset item with incorrect state`() {
        /* Given */
        val items = setOf(
                Item().apply { id = UUID.randomUUID(); status = Status.STARTED },
                Item().apply { id = UUID.randomUUID(); status = Status.PAUSED }
        )

        whenever(itemRepository.findByStatus(Status.STARTED, Status.PAUSED)).thenReturn(items.toVΛVΓ())

        /* When */
        updatePodcastBusiness.resetItemWithIncorrectState()

        /* Then */
        verify(itemRepository, times(2)).save(argWhere { it.status == Status.NOT_DOWNLOADED })
    }

    @Test
    fun `should check default status of update`() {
        assertThat(updatePodcastBusiness.isUpdating).isFalse()
    }

    @Nested
    @DisplayName("should update")
    inner class ShouldUpdate{

        @Test
        fun `all podcasts`() {
            /* Given */
            val now = ZonedDateTime.now()
            val podcast1 = Podcast().apply { title = "podcast1"; url = "http://foo.bar.com/foo1.xml" }
            val podcast2 = Podcast().apply { title = "podcast2"; url = "http://foo.bar.com/foo2.xml" }
            val podcast3 = Podcast().apply { title = "podcast3"; url = "http://foo.bar.com/foo3.xml" }
            val updater = mock<Updater>()
            val podcasts = setOf(podcast1, podcast2, podcast3)
            whenever(podcastRepository.findByUrlIsNotNull()).thenReturn(podcasts.toVΛVΓ())
            whenever(updaterSelector.of(argWhere { podcasts.map{it.url}.contains(it) })).thenReturn(updater)
            whenever(updater.notIn(any())).then { { item: Item -> !it.getArgument<Podcast>(0).contains(item) } }
            doAnswer { val p = it.getArgument<Podcast>(0); UpdatePodcastInformation(p, generateItems(10, p), updater.notIn(p)) }.whenever(updater).update(podcast3)
            doAnswer { val p = it.getArgument<Podcast>(0); UpdatePodcastInformation(p, p.items!!, updater.notIn(p)) }.whenever(updater).update(argWhere { it != podcast3 })
            whenever(validator.validate(any<Item>())).thenReturn(setOf<ConstraintViolation<Item>>())

            /* When */
            updatePodcastBusiness.updatePodcast()

            /* Then */
            assertThat(podcast1.lastUpdate).isNull()
            assertThat(podcast2.lastUpdate).isNull()
            assertThat(podcast3.lastUpdate).isBeforeOrEqualTo(ZonedDateTime.now()).isAfterOrEqualTo(now)
            assertThat(updatePodcastBusiness.lastFullUpdate).isNotNull()

            verify(podcastRepository, times(podcasts.size)).save(any())
            verify(validator, times(10)).validate(argWhere<Item> { podcast3.items!!.contains(it) })
        }

        @Test
        fun `and add no new item in podcast because every item already exists`() {
            /* Given */
            val item1 = Item()
            val item2 = Item()
            val item3 = Item()
            val uuid = UUID.randomUUID()
            val podcast = Podcast().apply {
                url = "http://an.superb.url/"
                title = "a title"
                id = uuid
                add(item1); add(item2); add(item3)
            }
            val updater = mock<Updater>()

            whenever(podcastRepository.findById(uuid)).thenReturn(Optional.of(podcast))
            whenever(updaterSelector.of(podcast.url)).thenReturn(updater)
            whenever(updater.update(any())).then {
                val p = it.getArgument<Podcast>(0)
                UpdatePodcastInformation(p, generateItems(10, p)) { false }
            }

            /* When */
            updatePodcastBusiness.updatePodcast(uuid)

            /* Then */
            assertThat(podcast.lastUpdate).isNull()
            verify(podcastRepository, never()).save(any())
        }

        @Test
        fun `a single podcast`() {
            /* Given */
            val now = ZonedDateTime.now()
            val uuid = UUID.randomUUID()
            val podcast = Podcast().apply { id = uuid; title = "podcast"; url = "http://foo.bar.com/foo1.xml" }
            val updater = mock<Updater>()
            val items = generateItems(10, podcast).toVΛVΓ()
            whenever(podcastRepository.findById(uuid)).thenReturn(Optional.of(podcast))
            whenever(updaterSelector.of(podcast.url)).thenReturn(updater)
            whenever(updater.notIn(any())).then { { item: Item -> !it.getArgument<Podcast>(0).contains(item) } }
            whenever(updater.update(any())).then {
                val p = it.getArgument<Podcast>(0)
                UpdatePodcastInformation(p, items.toJavaSet(), updater.notIn(p))
            }
            whenever(validator.validate(any<Item>())).thenReturn(setOf<ConstraintViolation<Item>>())

            /* When */
            updatePodcastBusiness.updatePodcast(uuid)

            /* Then */
            assertThat(podcast.lastUpdate)
                    .isAfterOrEqualTo(now)
                    .isBeforeOrEqualTo(ZonedDateTime.now())

            verify(podcastRepository, times(1)).save(podcast)
            verify(validator, times(10)).validate(argWhere<Item> { items.contains(it) })
        }

        @Test
        fun `and throw exception if podcast doesn't exist`() {
            /* Given */
            val id = UUID.randomUUID()

            /* When */
            assertThatThrownBy { updatePodcastBusiness.updatePodcast(id) }

            /* Then */
                    .isInstanceOf(RuntimeException::class.java)
                    .hasMessage("Podcast with ID $id not found")
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        fun `and not handle too long update`() {
            /* Given */
            val updater = mock<Updater>()
            val podcast1 = Podcast().apply {
                id = UUID.randomUUID()
                title = "podcast_async"
                url = "http://a.specific.url.com"
            }
            updatePodcastBusiness.setTimeOut(1, MILLISECONDS)
            whenever(updaterSelector.of(podcast1.url)).thenReturn(updater)
            whenever(updater.update(podcast1)).then { SECONDS.sleep(1) }
            whenever(podcastRepository.findById(any())).thenReturn(Optional.of(podcast1))

            /* When */
            updatePodcastBusiness.updatePodcast(UUID.randomUUID())

            /* Then */
            assertThat(podcast1.lastUpdate).isNull()
            verify(podcastRepository, times(1)).findById(any())
            verify(podcastRepository, never()).save(any())
            //verify(updater, times(1)).update(podcast1)
            //verifyNoMoreInteractions(updater)
        }

        @Nested
        @DisplayName("forced")
        inner class Forced {

            @Test
            fun `a podcast`() {
                /* Given */
                val now = ZonedDateTime.now()
                val p = Podcast().apply { id = UUID.randomUUID(); title = "podcast1"; url = "http://foo.bar.com" }
                val updater = mock<Updater>()
                whenever(updaterSelector.of(p.url)).thenReturn(updater)
                whenever(updater.notIn(any())).then {
                    { item: Item -> !it.getArgument<Podcast>(0).contains(item) }
                }
                whenever(updater.update(any())).then {
                    val podcast = it.getArgument<Podcast>(0)
                    UpdatePodcastInformation(podcast, generateItems(7, p), updater.notIn(podcast))
                }
                whenever(podcastRepository.findById(any())).thenReturn(Optional.of(p))
                whenever(podcastRepository.save(any())).thenReturn(p)
                whenever(validator.validate(any<Item>())).thenReturn(setOf<ConstraintViolation<Item>>())

                /* When */
                updatePodcastBusiness.forceUpdatePodcast(UUID.randomUUID())

                /* Then */
                assertThat(p.lastUpdate)
                        .isAfterOrEqualTo(now)
                        .isBeforeOrEqualTo(ZonedDateTime.now())
                verify(podcastRepository, times(2)).findById(any())
                verify(podcastRepository, times(2)).save(any())
            }


            @Test
            fun `and throw exception if podcast doesn't exist`() {
                /* Given */
                val id = UUID.randomUUID()

                /* When */
                assertThatThrownBy { updatePodcastBusiness.forceUpdatePodcast(id) }

                        /* Then */
                        .isInstanceOf(RuntimeException::class.java)
                        .hasMessage("Podcast with ID $id not found")
            }

        }

    }


    @Test
    fun `should get number of active count`() {
        /* Given */
        whenever(updateExecutor.activeCount).thenReturn(6)
        whenever(manualExecutor.activeCount).thenReturn(1)
        /* When */
        val numberOfActiveThread = updatePodcastBusiness.updaterActiveCount
        /* Then */
        assertThat(numberOfActiveThread).isEqualTo(7)
        verify(updateExecutor, times(1)).activeCount
        verify(manualExecutor, times(1)).activeCount
    }

    @Test
    fun `should delete cover`() {
        /* Given */
        val p = Podcast().apply { id = UUID.randomUUID() }
        val items = setOf(
                Item().apply { title = "Number1"; podcast = p },
                Item().apply { title = "Number2"; podcast = p },
                Item().apply { title = "Number3"; podcast = p }
        )

        items.map { it.title }
                .forEach { t -> Try { Files.createFile(Paths.get("/tmp/", t)) } }

        whenever(itemRepository.findAllToDelete(any())).thenReturn(items.toVΛVΓ())
        doAnswer { Option.just(Paths.get("/tmp/", it.getArgument<Item>(0).title)).toVΛVΓ() }
                .whenever(coverBusiness).getCoverPathOf(any<Item>())
        whenever(podcastServerParameters.limitToKeepCoverOnDisk()).thenReturn(ZonedDateTime.now())

        /* When */
        updatePodcastBusiness.deleteOldCover()

        /* Then */
        assertThat(Paths.get("/tmp/", "Number1")).doesNotExist()
        assertThat(Paths.get("/tmp/", "Number2")).doesNotExist()
        assertThat(Paths.get("/tmp/", "Number3")).doesNotExist()
    }

    private fun generateItems(number: Int, p: Podcast): Set<Item> {
        return (1..number)
                .map { Item().apply {
                    podcast = p
                    id = UUID.randomUUID()
                    fileName = "$it.mp3"
                    status = Status.FINISH
                } }
                .toSet()
    }

    companion object {
        private val ROOT_FOLDER = Paths.get("/tmp/podcast/")
    }

}
