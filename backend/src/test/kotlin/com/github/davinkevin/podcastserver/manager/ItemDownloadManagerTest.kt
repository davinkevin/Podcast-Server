package com.github.davinkevin.podcastserver.manager

import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.nhaarman.mockitokotlin2.*
import java.net.URI

/**
 * Created by kevin on 06/05/15
 */
//@ExtendWith(SpringExtension::class)
//@Import(ItemDownloadManager::class)
class ItemDownloadManagerTest {
//
//    @Autowired lateinit var template: MessagingTemplate
//    @Autowired lateinit var downloadRepository: DownloadRepository
//    @Autowired lateinit var podcastServerParameters: PodcastServerParameters
//    @Autowired lateinit var downloaderSelector: DownloaderSelector
//    @Autowired lateinit var extractorSelector: ExtractorSelector
//    @Autowired lateinit var downloaderExecutor: ThreadPoolTaskExecutor
//    @Autowired lateinit var itemDownloadManager: ItemDownloadManager
//
//    private val dItem: DownloadingItem = DownloadingItem (
//            id = UUID.randomUUID(),
//            title = "Title",
//            status = Status.NOT_DOWNLOADED,
//            url = URI("http://a.fake.url/with/file.mp4?param=1"),
//            numberOfFail = 0,
//            progression = 0,
//            podcast = DownloadingItem.Podcast(
//                    id = UUID.randomUUID(),
//                    title = "A Fake ffmpeg Podcast"
//            ),
//            cover = DownloadingItem.Cover(
//                    id = UUID.randomUUID(),
//                    url = URI("https://bar/foo/cover.jpg")
//            )
//    )
//
//    @BeforeEach
//    fun beforeEach() {
//        Mockito.reset(template)
//    }
//
//    @Nested
//    @DisplayName("on limit of download")
//    inner class OnLimitOfDownload {
//
//        @Test
//        fun `should get limit`() {
//            /* Given */
//            downloaderExecutor.corePoolSize = 3
//            /* When */
//            val nbOfDownload = itemDownloadManager.limitParallelDownload
//            /* Then */
//            assertThat(nbOfDownload).isEqualTo(3)
//        }
//
//        @Test
//        fun `should increase number of parallel download`() {
//            /* Given */
//            downloaderExecutor.corePoolSize = 3
//            /* When */
//            itemDownloadManager.setLimitParallelDownload(5)
//            /* Then */
//            assertThat(downloaderExecutor.corePoolSize).isEqualTo(5)
//            verify(template, times(1)).sendWaitingQueue(isAnEmptyQueue())
//        }
//
//        @Test
//        fun `should decrease number of parallel download`() {
//            /* Given */
//            downloaderExecutor.corePoolSize = 3
//            /* When */
//            itemDownloadManager.setLimitParallelDownload(1)
//            /* Then */
//            verify(template, times(1)).sendWaitingQueue(isAnEmptyQueue())
//        }
//    }
//
//    @Nested
//    @DisplayName("on queues")
//    inner class OnWaitingQueue {
//
//        @Test
//        fun `should have access to empty waiting queue`() {
//            assertThat(itemDownloadManager.waitingQueue).isEmpty()
//        }
//
//
//        @Test
//        fun `should have access to epty downloading queue`() {
//            assertThat(itemDownloadManager.downloadingQueue).isEmpty()
//        }
//
//    }
//
//    @Test
//    fun should_init_download_with_empty_list() {
//        /* Given */
//        val nowDate = ZonedDateTime.now()
//        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
//        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
//        whenever(downloadRepository.findAllToDownload(nowDate.toOffsetDateTime(), 5)).thenReturn(Flux.empty())
//        /* When */
//        itemDownloadManager.launchDownload()
//        /* Then */
//        verify(podcastServerParameters, times(1)).limitDownloadDate()
//        verify(podcastServerParameters, times(1)).numberOfTry
//        verify(template, times(1)).sendWaitingQueue(isAnEmptyQueue())
//    }
//
//    @Test
//    fun should_init_download_with_list_of_item_larger_than_download_limit() {
//        /* Given */
//        val nowDate = ZonedDateTime.now()
//
//        val item1 = dItem.copy(UUID.randomUUID())
//        val item2 = item1.copy(UUID.randomUUID())
//        val item3 = item1.copy(UUID.randomUUID())
//        val item4 = item1.copy(UUID.randomUUID())
//        val items = Flux.just(item1, item2, item3, item4)
//        val downloader = mock<Downloader>()
//
//        downloaderExecutor.corePoolSize = 3
//        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
//        whenever(downloadRepository.findAllToDownload(nowDate.toOffsetDateTime(), 5)).thenReturn(items)
//        whenever(extractorSelector.of(any())).thenReturn(PassThroughExtractor())
//        whenever(downloaderSelector.of(any())).thenReturn(downloader)
//        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
//
//        /* When */
//        itemDownloadManager.launchDownload()
//
//        /* Then */
//        verify(podcastServerParameters, times(1)).limitDownloadDate()
//        verify(podcastServerParameters, times(1)).numberOfTry
//        verify(downloaderSelector, times(3)).of(any())
//        verify(extractorSelector, times(3)).of(any())
//        verifyConvertAndSave(times(1))
//    }
//
////    @Test
////    fun should_relaunch_a_paused_download() {
////        /* Given */
////        val nowDate = ZonedDateTime.now()
////        val mockDownloader = mock<Downloader>()
////        val item = dItem.copy(id = UUID.randomUUID(), status = Status.NOT_DOWNLOADED)
////        itemDownloadManager.downloadingQueue = mapOf(Pair(item, mockDownloader))
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(downloadRepository.findAllToDownload(nowDate.toOffsetDateTime(), 5)).thenReturn(Flux.just(item))
////
////        /* When */
////        itemDownloadManager.launchDownload()
////
////        /* Then */
////        verify(podcastServerParameters, times(1)).limitDownloadDate()
////        verify(podcastServerParameters, times(1)).numberOfTry
////        verifyConvertAndSave(times(1))
////    }
//
////    @Test
////    fun should_stop_all_downloads() {
////        /* Given */
////        val nowDate = ZonedDateTime.now()
////        downloaderExecutor.corePoolSize = 3
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(downloadRepository.findAllToDownload(nowDate.toOffsetDateTime(), 5)).thenReturn(Flux.just(item1, item2, item3))
////        whenever(downloaderSelector.of(any())).thenReturn(NoOpDownloader())
////        whenever(extractorSelector.of(any())).thenReturn(PassThroughExtractor())
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        itemDownloadManager.stopAllDownload()
////
////        /* Then */
////        verify(downloader1).stopDownload()
////        verify(downloader2).stopDownload()
////        verify(downloader3).stopDownload()
////        verifyPostLaunchDownload()
////    }
//
//    @TestConfiguration
//    class LocalTestConfiguration {
//        @Bean fun mockTemplate() = mock<MessagingTemplate>()
//        @Bean fun mockDownloadRepository() = mock<DownloadRepository>()
//        @Bean fun mockPodcastServerParameters() = mock<PodcastServerParameters>()
//        @Bean fun mockDownloaderSelector() = mock<DownloaderSelector>()
//        @Bean fun mockExtractorSelector() = mock<ExtractorSelector>()
//        @Bean("DownloadExecutor") fun localDownloaderExecutor() = ThreadPoolTaskExecutor().apply {
//            corePoolSize = 3
//            setThreadNamePrefix("Downloader-")
//            initialize()
//        }
//    }
//
//
//
//    private fun generateDownloaderAndRegisterIt(anId: UUID): Pair<DownloadingItem, Downloader> {
//        val mockDownloader = mock<Downloader>()
//        val item = dItem.copy(id = UUID.randomUUID(), url = URI("http://gnerated.com/$anId"), numberOfFail = 0, status = Status.NOT_DOWNLOADED)
//        doAnswer { mockDownloader }
//                .whenever(downloaderSelector)
//                .of(DownloadingInformation(item, listOf(anId.toString()), "file-$anId.mp4", null))
//        return item to mockDownloader
//    }
//
//    @Test
//    fun should_pause_all_downloads() {
//        /* Given */
//        val nowDate = ZonedDateTime.now()
//        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
//        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
//        downloaderExecutor.corePoolSize = 3
//        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
//        whenever(downloadRepository.findAllToDownload(nowDate.toOffsetDateTime(), 5)).thenReturn(Flux.just(item1, item2))
//        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
//        whenever(extractorSelector.of(any())).thenReturn(PassThroughExtractor())
//        itemDownloadManager.launchDownload()
//
//        /* When */
//        itemDownloadManager.pauseAllDownload()
//
//        /* Then */
//        verify(downloader1).pauseDownload()
//        verify(downloader2).pauseDownload()
//        verifyPostLaunchDownload()
//    }
//
////    @Test
////    fun should_restart_all_downloads() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        whenever(downloader1.item).thenReturn(item1)
////        whenever(downloader2.item).thenReturn(item2)
////        item1.status = Status.PAUSED
////        item2.status = Status.PAUSED
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2).toVΛVΓ())
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        itemDownloadManager.restartAllDownload()
////
////        /* Then */
////        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
////            verify(downloader1).restartDownload()
////            verify(downloader2).restartDownload()
////        }
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_add_item_to_queue() {
////        /* Given */
////        val item = Item().apply { id = UUID.randomUUID(); status = Status.FINISH }
////        whenever(itemRepository.findById(item.id!!)).thenReturn(Optional.of(item))
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////
////        /* When */
////        itemDownloadManager.addItemToQueue(item.id!!)
////
////        /* Then */
////        verifyConvertAndSave()
////        verify(itemRepository).findById(item.id!!)
////        assertThat(itemDownloadManager.waitingQueue).isEmpty()
////    }
////
////    @Test
////    fun `should throw error if trying to add an unkown item to queue`() {
////        /* Given */
////        whenever(itemRepository.findById(any())).thenReturn(Optional.empty())
////
////        /* When */
////        assertThatThrownBy { itemDownloadManager.addItemToQueue(UUID.randomUUID()) }
////                .isInstanceOf(RuntimeException::class.java)
////                .hasMessageStartingWith("Item with ID ")
////                .hasMessageEndingWith("not found")
////
////        /* Then */
////        verifyConvertAndSave(never())
////        verify(itemRepository).findById(any())
////        assertThat(itemDownloadManager.waitingQueue).isEmpty()
////    }
////
////    @Test
////    fun should_remove_from_queue() {
////        /* Given */
////        val item = Item().apply { id = UUID.randomUUID() }
////        whenever(itemRepository.findById(item.id!!)).thenReturn(Optional.of(item))
////        /* When */
////        itemDownloadManager.removeItemFromQueue(item.id!!, true)
////        /* Then */
////        verify(itemRepository).findById(item.id!!)
////        verify(itemRepository).save(item)
////        verifyConvertAndSave(times(1))
////    }
////
////    @Test
////    fun should_remove_from_queue_an_unknown_item() {
////        /* Given */
////        val item = Item().apply { id = UUID.randomUUID() }
////        whenever(itemRepository.findById(item.id!!)).thenReturn(Optional.empty())
////
////        /* When */
////        assertThatThrownBy { itemDownloadManager.removeItemFromQueue(item.id!!, true) }
////                .isInstanceOf(RuntimeException::class.java)
////                .hasMessageStartingWith("Item with ID ")
////                .hasMessageEndingWith("not found")
////
////        /* Then */
////        verify(itemRepository).findById(item.id!!)
////        verifyConvertAndSave(times(0))
////    }
////
//    private fun verifyConvertAndSave(mode: VerificationMode = times(1)) {
//        verify(template, mode).sendWaitingQueue(
//                argWhere { it.containsAll(itemDownloadManager.waitingQueue) }
//        )
//    }
////
////    @Test
////    fun should_stop_a_current_download() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        itemDownloadManager.stopDownload(item2.id!!)
////
////        /* Then */
////        verify(downloader1, never()).stopDownload()
////        verify(downloader2, times(1)).stopDownload()
////        verify(downloader3, never()).stopDownload()
////        verifyPostLaunchDownload()
////    }
////
//    private fun verifyPostLaunchDownload() {
//        verify(podcastServerParameters, atLeast(1)).limitDownloadDate()
//        verify(extractorSelector, atLeast(1)).of(any())
//        verify(downloaderSelector, atLeast(1)).of(any())
//        verify(podcastServerParameters, atLeast(1)).numberOfTry
//        verifyConvertAndSave(atLeast(1))
//    }
////
////    @Test
////    fun should_pause_a_current_download() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        itemDownloadManager.pauseDownload(item2.id!!)
////
////        /* Then */
////        verify(downloader1, never()).pauseDownload()
////        verify(downloader2, times(1)).pauseDownload()
////        verify(downloader3, never()).pauseDownload()
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_toggle_on_a_STARTED_download() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////        item2.status = Status.STARTED
////        whenever(downloader2.item).thenReturn(item2)
////
////        /* When */
////        itemDownloadManager.toggleDownload(item2.id!!)
////
////        /* Then */
////        verify(downloader1, never()).pauseDownload()
////        verify(downloader2, times(1)).pauseDownload()
////        verify(downloader3, never()).pauseDownload()
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_toggle_on_a_PAUSED_download() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////        whenever(downloader2.item).thenReturn(item2)
////        item2.status = Status.PAUSED
////
////        /* When */
////        itemDownloadManager.toggleDownload(item2.id!!)
////
////        /* Then */
////        verify(downloader1, never()).restartDownload()
////        verify(downloader2, times(1)).restartDownload()
////        verify(downloader3, never()).restartDownload()
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_toggle_an_unknown_item() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2, downloader2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3, downloader3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////        item2.status = Status.PAUSED
////
////        /* When */
////        assertThatThrownBy { itemDownloadManager.toggleDownload(UUID.randomUUID()) }
////                .isInstanceOf(RuntimeException::class.java)
////                .hasMessageStartingWith("downloader not found for item with id")
////
////        /* Then */
////        verify(downloader1, never()).restartDownload()
////        verify(downloader2, never()).restartDownload()
////        verify(downloader3, never()).restartDownload()
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_remove_a_current_download() {
////        /* Given */
////        val downloader = mock<Downloader>()
////        val item = Item().apply { id = UUID.randomUUID(); pubDate = now(); url = "http://nowhere.else" }
////        itemDownloadManager.downloadingQueue = mapOf(Pair(item, downloader))
////
////        /* When */
////        itemDownloadManager.removeACurrentDownload(item.id!!)
////
////        /* Then */
////        assertThat(itemDownloadManager.downloadingQueue).hasSize(0)
////        verifyConvertAndSave(times(1))
////    }
////
////    @Test
////    fun should_move_item_in_queue() {
////        /* Given */
////        itemDownloadManager.addItemToQueue(ITEM_1)
////        itemDownloadManager.addItemToQueue(ITEM_2)
////        itemDownloadManager.addItemToQueue(ITEM_3)
////
////        /* When */
////        itemDownloadManager.moveItemInQueue(ITEM_2.id!!, 2)
////
////        /* Then */
////        assertThat(itemDownloadManager.waitingQueue)
////                .containsSequence(ITEM_1, ITEM_3, ITEM_2)
////
////        verify(template, times(4)).sendWaitingQueue(any())
////    }
////
////    @Test
////    fun should_do_nothing_on_non_present_item_movement() {
////        /* Given */
////        itemDownloadManager.addItemToQueue(ITEM_1)
////        itemDownloadManager.addItemToQueue(ITEM_2)
////        itemDownloadManager.addItemToQueue(ITEM_3)
////
////        /* When */
////        assertThatThrownBy { itemDownloadManager.moveItemInQueue(UUID.randomUUID(), 2) }
////                .isInstanceOf(RuntimeException::class.java)
////                .hasMessage("Moving element in waiting list not authorized : Element wasn't in the list")
////
////        /* Then */
////        assertThat(itemDownloadManager.waitingQueue).containsSequence(ITEM_1, ITEM_2, ITEM_3)
////        verify(template, times(3)).sendWaitingQueue(any())
////    }
////
////    @Test
////    fun should_remove_from_both_queue() {
////        /* Given */
////        val nowDate = now()
////        val (item1, downloader1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val item2 = Item().apply { id = UUID.randomUUID(); url = UUID.randomUUID().toString(); numberOfFail = 0}
////        val item3 = Item().apply { id = UUID.randomUUID(); url = UUID.randomUUID().toString(); numberOfFail = 0}
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        doAnswer { setOf(item1).toVΛVΓ() }.
////        doAnswer { setOf(item2, item3).toVΛVΓ() }
////                .whenever(itemRepository).findAllToDownload(nowDate, 5)
////
////        whenever(downloaderExecutor.corePoolSize).thenReturn(1)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        itemDownloadManager.removeItemFromQueueAndDownload(item1.id!!)
////        itemDownloadManager.removeItemFromQueueAndDownload(item3.id!!)
////
////        /* Then */
////        assertThat(itemDownloadManager.waitingQueue).hasSize(1)
////        verify(downloader1, times(1)).stopDownload()
////        verify(template, times(4)).sendWaitingQueue(any())
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_detect_if_is_in_downloading_queue() {
////        /* Given */
////        val nowDate = now()
////        val (item1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        val isIn = itemDownloadManager.isInDownloadingQueue(item1)
////        val isNotIn = itemDownloadManager.isInDownloadingQueue(Item.DEFAULT_ITEM)
////
////        /* Then */
////        assertThat(isIn).isTrue()
////        assertThat(isNotIn).isFalse()
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_get_downloading_item_list() {
////        /* Given */
////        val nowDate = now()
////        val (item1) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item2) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////        val (item3) = generateDownloaderAndRegisterIt(UUID.randomUUID())
////
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1, item2, item3).toVΛVΓ())
////        whenever(extractorSelector.of(any())).thenReturn(NoOpExtractor())
////        whenever(downloaderExecutor.corePoolSize).thenReturn(3)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        itemDownloadManager.launchDownload()
////
////        /* When */
////        val items = itemDownloadManager.downloadingItems
////
////        /* Then */
////        assertThat(items).contains(item1, item2, item3)
////        verifyPostLaunchDownload()
////    }
////
////    @Test
////    fun should_not_allowed_parallel_update() {
////        /* Given */
////        /* When */
////        itemDownloadManager.setLimitParallelDownload(5)
////        runAsync { itemDownloadManager.setLimitParallelDownload(10) }
////
////        /* Then */
////        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
////            verify(template, atLeast(2)).sendWaitingQueue(any())
////        }
////    }
////
////    @Test
////    fun `should handle error during extract`() {
////        val nowDate = now()
////        val item1 = Item().apply { id = UUID.randomUUID(); url = id.toString(); numberOfFail = 0}
////
////        whenever(downloaderExecutor.corePoolSize).thenReturn(1)
////        whenever(podcastServerParameters.numberOfTry).thenReturn(5)
////        whenever(podcastServerParameters.limitDownloadDate()).thenReturn(nowDate)
////        whenever(itemRepository.findAllToDownload(nowDate, 5)).thenReturn(setOf(item1).toVΛVΓ())
////        whenever(extractorSelector.of(item1.url)).thenReturn(ErrorExtractor())
////
////        /* When */
////        itemDownloadManager.launchDownload()
////
////        /* Then */
////        assertThat(itemDownloadManager.waitingQueue).hasSize(0)
////        verifyConvertAndSave(times(2))
////        verify(extractorSelector, times(1)).of(any())
////        verify(downloaderSelector, never()).of(any())
////    }
////
////    @AfterEach
////    fun afterEach() {
////        verify(podcastServerParameters, atLeast(1)).rootfolder
////        verifyNoMoreInteractions(template, itemRepository, podcastServerParameters, downloaderSelector, extractorSelector)
////    }
////
////    companion object {
////
////        private const val NUMBER_OF_DOWNLOAD = 3
////
////        private val ITEM_1 = Item().apply { id = UUID.randomUUID(); status = Status.NOT_DOWNLOADED; url = "http://now.where/" + 1; pubDate = now() }
////        private val ITEM_2 = Item().apply { id = UUID.randomUUID(); status = Status.STARTED; url = "http://now.where/" + 2; pubDate = now() }
////        private val ITEM_3 = Item().apply { id = UUID.randomUUID(); status = Status.PAUSED; url = "http://now.where/" + 3; pubDate = now() }
////    }
}

private fun isAnEmptyQueue() = argWhere<List<DownloadingItem>> { it.isEmpty() }

private class ErrorExtractor: Extractor {
    override fun extract(item: DownloadingItem): DownloadingInformation = throw RuntimeException("Error during extraction")
    override fun compatibility(url: URI): Int = 1
}
