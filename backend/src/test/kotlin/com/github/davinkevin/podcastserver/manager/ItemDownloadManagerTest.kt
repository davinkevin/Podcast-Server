package com.github.davinkevin.podcastserver.manager

import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.manager.selector.ExtractorSelector
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.manager.worker.noop.NoOpExtractor
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Status
import lan.dk.podcastserver.repository.ItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.verification.VerificationMode
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.ZonedDateTime.now
import java.util.*
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.TimeUnit

/**
 * Created by kevin on 06/05/15
 */
@ExtendWith(MockitoExtension::class)
class ItemDownloadManagerTest {

    @Mock lateinit var template: SimpMessagingTemplate
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var downloaderSelector: DownloaderSelector
    @Mock lateinit var extractorSelector: ExtractorSelector
    @Mock lateinit var downloaderExecutor: ThreadPoolTaskExecutor
    @InjectMocks lateinit var itemDownloadManager: ItemDownloadManager

    @Test
    fun should_get_limit_of_download() {
        /* Given */
        whenever(downloaderExecutor.corePoolSize).thenReturn(NUMBER_OF_DOWNLOAD)
        /* When */
        val nbOfDownload = itemDownloadManager.limitParallelDownload
        /* Then */
        assertThat(nbOfDownload).isEqualTo(NUMBER_OF_DOWNLOAD)
        verify(downloaderExecutor, times(1)).corePoolSize
    }

    @Test
    fun should_change_limit_of_download_sup() {
        /* Given */
        whenever(downloaderExecutor.corePoolSize).thenReturn(NUMBER_OF_DOWNLOAD)
        /* When */
        itemDownloadManager.setLimitParallelDownload(NUMBER_OF_DOWNLOAD + 1)
        /* Then */
        verify(template, times(1)).convertAndSend(eq("/topic/waiting"), isAnEmptyQueue())
    }

    @Test
    fun should_change_limit_of_download_for_less() {
        /* Given */
        whenever(downloaderExecutor.corePoolSize).thenReturn(NUMBER_OF_DOWNLOAD)
        /* When */
        itemDownloadManager.setLimitParallelDownload(NUMBER_OF_DOWNLOAD - 1)
        /* Then */
        verify(template, times(1))
                .convertAndSend(eq("/topic/waiting"), isAnEmptyQueue())
    }

    @Test
    fun should_get_empty_waiting_queue() {
        assertThat(itemDownloadManager.waitingQueue)
                .isNotNull
                .isEmpty()
    }

    @Test
    fun should_get_empty_downloading_queue() {
        assertThat(itemDownloadManager.downloadingQueue)
                .isNotNull
                .isEmpty()
    }

    @Test
    fun should_get_number_of_current_download() {
        assertThat(itemDownloadManager.numberOfCurrentDownload).isEqualTo(0)
    }

    @Test
    fun should_init_download_with_empty_list() {
        /* Given */
        val nowDate = now()
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(itemRepository.findAllToDownload(nowDate, 5))
                .thenReturn(setOf<Item>().toVΛVΓ())
        /* When */
        itemDownloadManager.launchDownload()
        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate()
        verify(podcastServerParameters, times(1)).numberOfTry
        verify(itemRepository, times(1)).findAllToDownload(nowDate, 5)
        verify(template, times(1)).convertAndSend(eq("/topic/waiting"), isAnEmptyQueue())
    }

    @Test
    fun should_init_download_with_list_of_item_larger_than_download_limit() {
        /* Given */
        val nowDate = now()

        val item1 = Item().apply { id = UUID.randomUUID(); url = "1"; status = Status.NOT_DOWNLOADED }
        val item2 = Item().apply { id = UUID.randomUUID(); url = "2"; status = Status.NOT_DOWNLOADED }
        val item3 = Item().apply { id = UUID.randomUUID(); url = "3"; status = Status.NOT_DOWNLOADED }
        val item4 = Item().apply { id = UUID.randomUUID(); url = "4"; status = Status.NOT_DOWNLOADED }
        val items = setOf(item1, item2, item3, item4).toVΛVΓ()
        val downloader = mock<Downloader>()

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(items)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        whenever(downloaderSelector.of(any())).thenReturn(downloader)
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)

        /* When */
        itemDownloadManager.launchDownload()

        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate()
        verify(podcastServerParameters, times(1)).numberOfTry
        verify(itemRepository, times(1)).findAllToDownload(nowDate, 5)
        verify(downloaderSelector, times(3)).of(any())
        verify(extractorSelector, times(3)).of(any())
        verifyConvertAndSave(times(1))
    }

//    @Test
//    fun should_relaunch_a_paused_download() {
//        /* Given */
//        val nowDate = ZonedDateTime.now()
//        val mockDownloader = mock<Downloader>()
//        val item = Item().apply { id = UUID.randomUUID(); status = Status.NOT_DOWNLOADED }
//        itemDownloadManager._downloadingQueue = mapOf(Pair(item, mockDownloader))
//        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
//        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
//        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item).toVΛVΓ())
//
//        /* When */
//        itemDownloadManager.launchDownload()
//
//        /* Then */
//        verify(podcastServerParameters, times(1)).limitDownloadDate()
//        verify(podcastServerParameters, times(1)).numberOfTry
//        verify(itemRepository, times(1)).findAllToDownload(any(), eq(5))
//        verifyConvertAndSave(times(1))
//    }

    @Test
    fun should_stop_all_downloads() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(extractorSelector.of(anyString())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.stopAllDownload()

        /* Then */
        verify(downloader1).stopDownload()
        verify(downloader2).stopDownload()
        verify(downloader3).stopDownload()
        verifyPostLaunchDownload()
    }

    private fun generateDownloaderAndRegisterIt(anId: UUID): Pair<Item, Downloader> {
        val mockDownloader = mock<Downloader>()
        val item = Item().apply { id = anId; url = anId.toString(); numberOfFail = 0}
        doAnswer { mockDownloader }.whenever(downloaderSelector).of(DownloadingItem(item, listOf(anId.toString()), null, null))
        return Pair(item, mockDownloader)
    }

    @Test
    fun should_pause_all_downloads() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2).toVΛVΓ())
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.pauseAllDownload()

        /* Then */
        verify(downloader1).pauseDownload()
        verify(downloader2).pauseDownload()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_restart_all_downloads() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        whenever(downloader1.item).thenReturn(item1)
        whenever(downloader2.item).thenReturn(item2)
        item1.status = Status.PAUSED
        item2.status = Status.PAUSED
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2).toVΛVΓ())
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.restartAllDownload()

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            verify(downloader1).restartDownload()
            verify(downloader2).restartDownload()
        }
        verifyPostLaunchDownload()
    }

    @Test
    fun should_add_item_to_queue() {
        /* Given */
        val item = Item().apply { id = UUID.randomUUID(); status = Status.FINISH }
        whenever(itemRepository.findById(item.id)).thenReturn(Optional.of(item))
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)

        /* When */
        itemDownloadManager.addItemToQueue(item.id)

        /* Then */
        verifyConvertAndSave()
        verify(itemRepository).findById(item.id)
        assertThat(itemDownloadManager.waitingQueue).isEmpty()
    }

    @Test
    fun should_not_treat_item_in_waiting_list() {
        /* Given */
        val item = Item().apply { id = UUID.randomUUID() }
        itemDownloadManager.addItemToQueue(item)

        /* When */
        itemDownloadManager.addItemToQueue(item)

        /* Then */
        verifyConvertAndSave()
    }


    @Test
    fun `should throw error if trying to add an unkown item to queue`() {
        /* Given */
        whenever(itemRepository.findById(any())).thenReturn(Optional.empty())

        /* When */
        assertThatThrownBy { itemDownloadManager.addItemToQueue(UUID.randomUUID()) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageStartingWith("Item with ID ")
                .hasMessageEndingWith("not found")

        /* Then */
        verifyConvertAndSave(never())
        verify(itemRepository).findById(any())
        assertThat(itemDownloadManager.waitingQueue).isEmpty()
    }

    @Test
    fun should_remove_from_queue() {
        /* Given */
        val item = Item().apply { id = UUID.randomUUID() }
        whenever(itemRepository.findById(item.id)).thenReturn(Optional.of(item))
        /* When */
        itemDownloadManager.removeItemFromQueue(item.id, true)
        /* Then */
        verify(itemRepository).findById(item.id)
        verify(itemRepository).save(item)
        verifyConvertAndSave(times(1))
    }

    @Test
    fun should_remove_from_queue_an_unknown_item() {
        /* Given */
        val item = Item().apply { id = UUID.randomUUID() }
        whenever(itemRepository.findById(item.id)).thenReturn(Optional.empty())

        /* When */
        assertThatThrownBy { itemDownloadManager.removeItemFromQueue(item.id, true) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageStartingWith("Item with ID ")
                .hasMessageEndingWith("not found")

        /* Then */
        verify(itemRepository).findById(item.id)
        verifyConvertAndSave(times(0))
    }

    private fun verifyConvertAndSave(mode: VerificationMode = times(1)) {
        verify(template, mode).convertAndSend(
                eq("/topic/waiting"),
                argWhere<io.vavr.collection.Queue<Item>> { it.containsAll(itemDownloadManager.waitingQueue) }
        )
    }

    @Test
    fun should_check_if_can_be_reseted() {
        /* Given */
        whenever(podcastServerParameters.numberOfTry).thenReturn(3)

        /* When */
        val isResetable = itemDownloadManager.canBeReset(Item().apply { numberOfFail = 2 })
        val isNotResetable = itemDownloadManager.canBeReset(Item().apply { numberOfFail = 4 })

        /* Then */
        assertThat(isResetable).isTrue()
        assertThat(isNotResetable).isFalse()
        verify(podcastServerParameters, times(2)).numberOfTry

    }

    @Test
    fun should_stop_a_current_download() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.stopDownload(item2.id)

        /* Then */
        verify(downloader1, never()).stopDownload()
        verify(downloader2, times(1)).stopDownload()
        verify(downloader3, never()).stopDownload()
        verifyPostLaunchDownload()
    }

    private fun verifyPostLaunchDownload() {
        verify(itemRepository, atLeast(1)).findAllToDownload(any(), eq(5))
        verify(podcastServerParameters, atLeast(1)).limitDownloadDate()
        verify(extractorSelector, atLeast(1)).of(anyString())
        verify(downloaderSelector, atLeast(1)).of(any())
        verify(podcastServerParameters, atLeast(1)).numberOfTry
        verifyConvertAndSave(atLeast(1))
    }

    @Test
    fun should_pause_a_current_download() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.pauseDownload(item2.id)

        /* Then */
        verify(downloader1, never()).pauseDownload()
        verify(downloader2, times(1)).pauseDownload()
        verify(downloader3, never()).pauseDownload()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_restart_a_current_download() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(anyString())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()
        whenever(downloader2.item).thenReturn(item2)

        /* When */
        itemDownloadManager.restartDownload(item2.id)

        /* Then */
        verify(downloader1, never()).restartDownload()
        verify(downloader2, times(1)).restartDownload()
        verify(downloader3, never()).restartDownload()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_toggle_on_a_STARTED_download() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()
        item2.status = Status.STARTED
        whenever(downloader2.item).thenReturn(item2)

        /* When */
        itemDownloadManager.toggleDownload(item2.id)

        /* Then */
        verify(downloader1, never()).pauseDownload()
        verify(downloader2, times(1)).pauseDownload()
        verify(downloader3, never()).pauseDownload()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_toggle_on_a_PAUSED_download() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()
        whenever(downloader2.item).thenReturn(item2)
        item2.status = Status.PAUSED

        /* When */
        itemDownloadManager.toggleDownload(item2.id)

        /* Then */
        verify(downloader1, never()).restartDownload()
        verify(downloader2, times(1)).restartDownload()
        verify(downloader3, never()).restartDownload()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_toggle_an_unknown_item() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()
        item2.status = Status.PAUSED

        /* When */
        assertThatThrownBy { itemDownloadManager.toggleDownload(UUID.randomUUID()) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageStartingWith("Item not with")
                .hasMessageEndingWith("not found in download list")

        /* Then */
        verify(downloader1, never()).restartDownload()
        verify(downloader2, never()).restartDownload()
        verify(downloader3, never()).restartDownload()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_remove_a_current_download() {
        /* Given */
        val downloader = mock<Downloader>()
        val item = Item().apply { id = UUID.randomUUID(); pubDate = now(); url = "http://nowhere.else" }
        itemDownloadManager._downloadingQueue = mapOf(Pair(item, downloader))

        /* When */
        itemDownloadManager.removeACurrentDownload(item)

        /* Then */
        assertThat(itemDownloadManager._downloadingQueue).hasSize(0)
        verifyConvertAndSave(times(1))
    }

    @Test
    fun should_move_item_in_queue() {
        /* Given */
        itemDownloadManager.addItemToQueue(ITEM_1)
        itemDownloadManager.addItemToQueue(ITEM_2)
        itemDownloadManager.addItemToQueue(ITEM_3)

        /* When */
        itemDownloadManager.moveItemInQueue(ITEM_2.id, 2)

        /* Then */
        assertThat(itemDownloadManager.waitingQueue)
                .containsSequence(ITEM_1, ITEM_3, ITEM_2)

        verify(template, times(4)).convertAndSend(eq("/topic/waiting"), any<io.vavr.collection.Queue<Item>>())
    }

    @Test
    fun should_do_nothing_on_non_present_item_movement() {
        /* Given */
        itemDownloadManager.addItemToQueue(ITEM_1)
        itemDownloadManager.addItemToQueue(ITEM_2)
        itemDownloadManager.addItemToQueue(ITEM_3)

        /* When */
        assertThatThrownBy { itemDownloadManager.moveItemInQueue(UUID.randomUUID(), 2) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Moving element in waiting list not authorized : Element wasn't in the list")

        /* Then */
        assertThat(itemDownloadManager.waitingQueue).containsSequence(ITEM_1, ITEM_2, ITEM_3)
        verify(template, times(3)).convertAndSend(eq("/topic/waiting"), any<io.vavr.collection.Queue<Item>>())
    }

    @Test
    fun should_reset_current_download() {
        /* Given */
        val nowDate = now()
        val (item) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item).toVΛVΓ())

        whenever(podcastServerParameters.numberOfTry).thenReturn(3)
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.resetDownload(item)

        /* Then */
        assertThat(item.numberOfFail).isEqualTo(1)
        verify(downloaderSelector, times(2)).of(DownloadingItem(item, listOf(item.url), null, null))
        verifyPostLaunchDownload()
    }

    @Test
    fun should_remove_from_both_queue() {
        /* Given */
        val nowDate = now()
        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val item2 = Item().apply { id = UUID.randomUUID(); url = UUID.randomUUID().toString(); numberOfFail = 0}
        val item3 = Item().apply { id = UUID.randomUUID(); url = UUID.randomUUID().toString(); numberOfFail = 0}

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        doAnswer { setOf(item1).toVΛVΓ() }.
        doAnswer { setOf(item2, item3).toVΛVΓ() }
                .whenever(itemRepository).findAllToDownload(nowDate, 5)

        whenever(downloaderExecutor.corePoolSize).thenReturn(1)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.removeItemFromQueueAndDownload(item1)
        itemDownloadManager.removeItemFromQueueAndDownload(item3)

        /* Then */
        assertThat(itemDownloadManager._waitingQueue).hasSize(1)
        verify(downloader1, times(1)).stopDownload()
        verify(template, times(4)).convertAndSend(eq("/topic/waiting"), any<io.vavr.collection.Queue<Item>>())
        verifyPostLaunchDownload()
    }

    @Test
    fun should_detect_if_is_in_downloading_queue() {
        /* Given */
        val nowDate = now()
        val (item1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        val isIn = itemDownloadManager.isInDownloadingQueue(item1)
        val isNotIn = itemDownloadManager.isInDownloadingQueue(Item.DEFAULT_ITEM)

        /* Then */
        assertThat(isIn).isTrue()
        assertThat(isNotIn).isFalse()
        verifyPostLaunchDownload()
    }

    @Test
    fun should_get_downloading_item_list() {
        /* Given */
        val nowDate = now()
        val (item1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        itemDownloadManager.launchDownload()

        /* When */
        val items = itemDownloadManager.itemsInDownloadingQueue

        /* Then */
        assertThat(items).contains(item1, item2, item3)
        verifyPostLaunchDownload()
    }

    @Test
    fun should_find_item_in_downloading_queue() {
        /* Given */
        val nowDate = now()
        val (item1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val (item3) = generateDownloaderAndRegisterIt(UUID.randomUUID())

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        itemDownloadManager.launchDownload()

        /* When */
        val item = itemDownloadManager.getItemInDownloadingQueue(item1.id)

        /* Then */
        assertThat(item).isSameAs(item1)
        verifyPostLaunchDownload()
    }

    @Test
    fun should_return_null_if_item_not_found_in_downloading_queue() {
        /* Given */
        val downloader = mock<Downloader>()
        val item1 = Item().apply { id = UUID.randomUUID(); status = Status.NOT_DOWNLOADED; url = "http://now.where/"+1; pubDate = now() }
        val item2 = Item().apply { id = UUID.randomUUID(); status = Status.NOT_DOWNLOADED; url = "http://now.where/"+2; pubDate = now() }
        val item3 = Item().apply { id = UUID.randomUUID(); status = Status.NOT_DOWNLOADED; url = "http://now.where/"+3; pubDate = now() }
        itemDownloadManager._downloadingQueue = mapOf(
                Pair(item1, downloader),
                Pair(item2, downloader),
                Pair(item3, downloader)
        )

        /* When */
        val item = itemDownloadManager.getItemInDownloadingQueue(UUID.randomUUID())

        /* Then */
        assertThat(item).isEqualTo(Item.DEFAULT_ITEM)
    }

    @Test
    fun should_not_allowed_parallel_update() {
        /* Given */
        /* When */
        itemDownloadManager.setLimitParallelDownload(5)
        runAsync { itemDownloadManager.setLimitParallelDownload(10) }

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            verify(template, atLeast(2)).convertAndSend(eq("/topic/waiting"), any<io.vavr.collection.Queue<Item>>())
        }
    }

    @Test
    fun should_clear_waiting_list() {
        /* Given */
        val nowDate = now()
        val (item1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
        val item2 = Item().apply { id = UUID.randomUUID(); url = id.toString(); numberOfFail = 0}
        val item3 = Item().apply { id = UUID.randomUUID(); url = id.toString(); numberOfFail = 0}

        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5))
                .thenReturn(setOf(item1).toVΛVΓ())
                .thenReturn(setOf(item2, item3).toVΛVΓ())
        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
        whenever(downloaderExecutor.corePoolSize).thenReturn(1)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        itemDownloadManager.launchDownload()
        itemDownloadManager.launchDownload()

        /* When */
        itemDownloadManager.clearWaitingQueue()

        /* Then */
        assertThat(itemDownloadManager.waitingQueue).hasSize(0)
        verifyPostLaunchDownload()
    }

    @Test
    fun `should handle error during extract`() {
        val nowDate = now()
        val item1 = Item().apply { id = UUID.randomUUID(); url = id.toString(); numberOfFail = 0}

        whenever(downloaderExecutor.corePoolSize).thenReturn(1)
        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1).toVΛVΓ())
        whenever(extractorSelector.of(item1.url)).thenReturn(ErrorExtractor())

        /* When */
        itemDownloadManager.launchDownload()

        /* Then */
        assertThat(itemDownloadManager.waitingQueue).hasSize(0)
        verifyConvertAndSave(times(2))
        verify(extractorSelector, times(1)).of(any())
        verify(downloaderSelector, never()).of(any())
    }

    @Test
    fun `should do nothing when resetting download which is not in downloading queue`() {
        /* Given */
        /* When */
        itemDownloadManager.resetDownload(Item.DEFAULT_ITEM)

        /* Then */
        verifyConvertAndSave(never())
    }

    @Test
    fun `should do nothing when resetting download which has been already resetted too many times`() {
        /* Given */
        val downloader = mock<Downloader>()
        val anItem = Item().apply {
            numberOfFail = 4
        }
        whenever(podcastServerParameters.numberOfTry).thenReturn(3)
        itemDownloadManager._downloadingQueue = mapOf(Pair(anItem, downloader))
        /* When */
        itemDownloadManager.resetDownload(anItem)

        /* Then */
        verifyConvertAndSave(never())
    }


    @AfterEach
    fun afterEach() {
        verify(podcastServerParameters, atLeast(1)).rootfolder
        verifyNoMoreInteractions(template, itemRepository, podcastServerParameters, downloaderSelector, extractorSelector)
    }

    companion object {

        private const val NUMBER_OF_DOWNLOAD = 3

        private val ITEM_1 = Item().apply { id = UUID.randomUUID(); status = Status.NOT_DOWNLOADED; url = "http://now.where/" + 1; pubDate = now() }
        private val ITEM_2 = Item().apply { id = UUID.randomUUID(); status = Status.STARTED; url = "http://now.where/" + 2; pubDate = now() }
        private val ITEM_3 = Item().apply { id = UUID.randomUUID(); status = Status.PAUSED; url = "http://now.where/" + 3; pubDate = now() }
    }
}

private fun isAnEmptyQueue() = argWhere<io.vavr.collection.Queue<Item>> { it.isEmpty }

private class ErrorExtractor: Extractor {
    override fun extract(item: Item) = throw RuntimeException("Error during extraction")
    override fun compatibility(url: String?) = 1
}