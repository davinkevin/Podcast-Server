package lan.dk.podcastserver.manager;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.downloader.Downloader;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector;
import com.github.davinkevin.podcastserver.manager.selector.ExtractorSelector;
import lan.dk.podcastserver.manager.worker.noop.NoOpExtractor;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import static com.jayway.awaitility.Awaitility.await;
import static io.vavr.API.List;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 06/05/15
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemDownloadManagerTest {

    private static final Integer NUMBER_OF_DOWNLOAD = 3;

    private @Captor ArgumentCaptor<String> stringArgumentCaptor;
    private @Captor ArgumentCaptor<Queue<Item>> queueArgumentCaptor;
    private @Captor ArgumentCaptor<DownloadingItem> downloadingItemArgumentCaptor;
    private @Captor ArgumentCaptor<Item> itemArgumentCaptor;
    private @Captor ArgumentCaptor<UUID> integerArgumentCaptor;

    private @Mock SimpMessagingTemplate template;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock DownloaderSelector downloaderSelector;
    private @Mock ExtractorSelector extractorSelector;
    private @Mock ThreadPoolTaskExecutor downloaderExecutor;

    private @InjectMocks ItemDownloadManager itemDownloadManager;

    private static final Item ITEM_1 = Item.builder().id(UUID.randomUUID()).status(Status.NOT_DOWNLOADED).url("http://now.where/" + 1).pubDate(ZonedDateTime.now()).build();
    private static final Item ITEM_2 = Item.builder().id(UUID.randomUUID()).status(Status.STARTED).url("http://now.where/" + 2).pubDate(ZonedDateTime.now()).build();
    private static final Item ITEM_3 = Item.builder().id(UUID.randomUUID()).status(Status.PAUSED).url("http://now.where/" + 3).pubDate(ZonedDateTime.now()).build();

    @Test
    public void should_get_limit_of_download () {
        /* Given */
        when(downloaderExecutor.getCorePoolSize()).thenReturn(NUMBER_OF_DOWNLOAD);
        /* When */
        Integer nbOfDownload = itemDownloadManager.getLimitParallelDownload();
        /* Then */
        assertThat(nbOfDownload).isEqualTo(NUMBER_OF_DOWNLOAD);
        verify(downloaderExecutor, times(1)).getCorePoolSize();
    }

    @Test
    public void should_change_limit_of_download_sup () {
        /* Given */
        when(downloaderExecutor.getCorePoolSize()).thenReturn(NUMBER_OF_DOWNLOAD);
        /* When */
        itemDownloadManager.setLimitParallelDownload(NUMBER_OF_DOWNLOAD + 1);
        /* Then */
        verify(template, times(1)).convertAndSend(eq("/topic/waiting"), queueArgumentCaptor.capture());
        assertThat(queueArgumentCaptor.getValue()).isNotNull().isEmpty();
    }

    @Test
    public void should_change_limit_of_download_for_less () {
        /* Given */
        when(downloaderExecutor.getCorePoolSize()).thenReturn(NUMBER_OF_DOWNLOAD);
        /* When */
        itemDownloadManager.setLimitParallelDownload(NUMBER_OF_DOWNLOAD - 1);
        /* Then */
        verify(template, times(1)).convertAndSend(eq("/topic/waiting"), queueArgumentCaptor.capture());
        assertThat(queueArgumentCaptor.getValue()).isNotNull().isEmpty();

    }

    @Test
    public void should_get_empty_waiting_queue() {
        assertThat(itemDownloadManager.getWaitingQueue()).isNotNull().isEmpty();
    }

    @Test
    public void should_get_empty_downloading_queue() {
        assertThat(itemDownloadManager.getDownloadingQueue()).isNotNull().isEmpty();
    }

    @Test
    public void should_get_number_of_current_download() {
        assertThat(itemDownloadManager.getNumberOfCurrentDownload()).isEqualTo(0);
    }

    @Test
    public void should_init_download_with_empty_list() throws URISyntaxException {
        /* Given */
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.empty());
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        /* When */
        itemDownloadManager.launchDownload();
        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate();
        verify(podcastServerParameters, times(1)).getNumberOfTry();
        verify(itemRepository, times(1)).findAllToDownload(any(), eq(5));
        verify(template, times(1)).convertAndSend(stringArgumentCaptor.capture(), queueArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualTo("/topic/waiting");
    }

    @Test
    public void should_init_download_with_list_of_item_larger_than_download_limit() throws URISyntaxException {
        /* Given */
        Item item1 = Item.builder().id(UUID.randomUUID()).url("1").status(Status.NOT_DOWNLOADED).build();
        Item item2 = Item.builder().id(UUID.randomUUID()).url("2").status(Status.NOT_DOWNLOADED).build();
        Item item3 = Item.builder().id(UUID.randomUUID()).url("3").status(Status.NOT_DOWNLOADED).build();
        Item item4 = Item.builder().id(UUID.randomUUID()).url("4").status(Status.NOT_DOWNLOADED).build();
        Set<Item> items = HashSet.of(item1, item2, item3, item4);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(items);
        Downloader downloader = mock(Downloader.class);
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        when(downloaderSelector.of(any(DownloadingItem.class))).thenReturn(downloader);
        when(downloader.setDownloadingItem(any())).thenReturn(downloader);
        when(downloader.setItemDownloadManager(any())).thenReturn(downloader);
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        /* When */
        itemDownloadManager.launchDownload();

        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate();
        verify(podcastServerParameters, times(1)).getNumberOfTry();
        verify(itemRepository, times(1)).findAllToDownload(any(), eq(5));
        verify(downloaderSelector, times(3)).of(downloadingItemArgumentCaptor.capture());
        verify(extractorSelector, times(3)).of(anyString());
        assertThat(downloadingItemArgumentCaptor.getAllValues()).hasSize(3);
        verifyConvertAndSave(times(1));
    }

    @Test
    public void should_relaunch_a_paused_download() throws URISyntaxException {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        final Item item = Item.builder().id(UUID.randomUUID()).status(Status.NOT_DOWNLOADED).build();
        itemDownloadManager.getDownloadingQueue().put(item, mockDownloader);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(item));

        /* When */
        itemDownloadManager.launchDownload();

        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate();
        verify(podcastServerParameters, times(1)).getNumberOfTry();
        verify(itemRepository, times(1)).findAllToDownload(any(), eq(5));
        verifyConvertAndSave(times(1));
    }

    @Test
    public void should_stop_all_downloads() {
        /* Given */
        Tuple2<Item, Downloader> mockDownloader1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> mockDownloader2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> mockDownloader3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(mockDownloader1._1(), mockDownloader2._1(), mockDownloader3._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.stopAllDownload();

        /* Then */
        verify(mockDownloader1._2(), times(1)).stopDownload();
        verify(mockDownloader2._2(), times(1)).stopDownload();
        verify(mockDownloader3._2(), times(1)).stopDownload();
        verifyPostLaunchDownload();
    }

    private Tuple2<Item, Downloader> generateDownloaderAndRegisterIt(UUID id) {
        final Downloader mockDownloader = mock(Downloader.class);
        Item item = Item.builder().id(id).url(id.toString()).numberOfFail(0).build();
        when(mockDownloader.getItem()).thenReturn(item);
        when(downloaderSelector.of(eq(DownloadingItem.builder().item(item).urls(List(id.toString())).build()))).thenReturn(mockDownloader);
        when(mockDownloader.setDownloadingItem(any())).thenReturn(mockDownloader);
        when(mockDownloader.setItemDownloadManager(any())).thenReturn(mockDownloader);
        when(mockDownloader.getItem()).thenReturn(item);
        return Tuple.of(item, mockDownloader);
    }

    @Test
    public void should_pause_all_downloads() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1()));
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.pauseAllDownload();

        /* Then */
        verify(entry1._2(), times(1)).pauseDownload();
        verify(entry2._2(), times(1)).pauseDownload();
        verifyPostLaunchDownload();
    }

    @Test
    public void should_restart_all_downloads() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        entry1._1().setStatus(Status.PAUSED);
        entry2._1().setStatus(Status.PAUSED);
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.restartAllDownload();

        /* Then */
        await().atMost(5, SECONDS).until(() -> {
            verify(entry2._2(), atLeast(1)).restartDownload();
            verify(entry2._2(), atLeast(1)).restartDownload();
        });
        verifyPostLaunchDownload();
    }

    @Test
    public void should_add_item_to_queue() throws URISyntaxException {
        /* Given */
        Item item = Item.builder().id(UUID.randomUUID()).status(Status.FINISH).build();
        when(itemRepository.findById(any(UUID.class))).thenReturn(Optional.of(item));
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);

        /* When */
        itemDownloadManager.addItemToQueue(item.getId());

        /* Then */
        verifyConvertAndSave(times(1));
        verify(itemRepository, times(1)).findById(eq(item.getId()));
        assertThat(itemDownloadManager.getWaitingQueue()).isEmpty();
    }

    @Test
    public void should_not_treat_item_in_waiting_list() {
        /* Given */
        Item item = new Item().setId(UUID.randomUUID());
        itemDownloadManager.addItemToQueue(item);

        /* When */
        itemDownloadManager.addItemToQueue(item);

        /* Then */
        verifyConvertAndSave(times(1));
    }

    @Test
    public void should_remove_from_queue() {
        /* Given */
        final Item item = new Item().setId(UUID.randomUUID());
        when(itemRepository.findById(any(UUID.class))).thenReturn(Optional.of(item));

        /* When */ itemDownloadManager.removeItemFromQueue(item.getId(), true);
        /* Then */
        verify(itemRepository, times(1)).findById(integerArgumentCaptor.capture());
        assertThat(integerArgumentCaptor.getValue()).isEqualTo(item.getId());
        verify(itemRepository, times(1)).save(eq(item));
        verifyConvertAndSave(times(1));
    }

    private void verifyConvertAndSave(VerificationMode mode) {
        verify(template, mode).convertAndSend(stringArgumentCaptor.capture(), queueArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualTo("/topic/waiting");
        assertThat(queueArgumentCaptor.getValue()).containsAll(itemDownloadManager.getWaitingQueue());
    }

    @Test
    public void should_check_if_can_be_reseted () {
        /* Given */ when(podcastServerParameters.getNumberOfTry()).thenReturn(3);
        /* When */
        Boolean isResetable = itemDownloadManager.canBeReset(new Item().setNumberOfFail(2));
        Boolean isNotResetable = itemDownloadManager.canBeReset(new Item().setNumberOfFail(4));

        /* Then */
        assertThat(isResetable).isTrue();
        assertThat(isNotResetable).isFalse();
        verify(podcastServerParameters, times(2)).getNumberOfTry();

    }

    @Test
    public void should_stop_a_current_download() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.stopDownload(entry2._1().getId());

        /* Then */
        verify(entry1._2(), never()).stopDownload();
        verify(entry2._2(), times(1)).stopDownload();
        verify(entry3._2(), never()).stopDownload();
        verifyPostLaunchDownload();
    }

    protected void verifyPostLaunchDownload() {
        verify(itemRepository, atLeast(1)).findAllToDownload(any(), eq(5));
        verify(podcastServerParameters, atLeast(1)).limitDownloadDate();
        verify(extractorSelector, atLeast(1)).of(anyString());
        verify(downloaderSelector, atLeast(1)).of(any(DownloadingItem.class));
        verify(podcastServerParameters, atLeast(1)).getNumberOfTry();
        verifyConvertAndSave(atLeast(1));
    }

    @Test
    public void should_pause_a_current_download() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */ itemDownloadManager.pauseDownload(entry2._1().getId());

        /* Then */
        verify(entry1._2(), never()).pauseDownload();
        verify(entry2._2(), times(1)).pauseDownload();
        verify(entry3._2(), never()).pauseDownload();
        verifyPostLaunchDownload();
    }

    @Test
    public void should_restart_a_current_download() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.restartDownload(entry2._1().getId());

        /* Then */
        verify(entry1._2(), never()).restartDownload();
        verify(entry2._2(), times(1)).restartDownload();
        verify(entry3._2(), never()).restartDownload();
        verifyPostLaunchDownload();
    }

    @Test
    public void should_toogle_on_a_STARTED_download() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        itemDownloadManager.launchDownload();
        entry2._1().setStatus(Status.STARTED);

        /* When */
        itemDownloadManager.toggleDownload(entry2._1().getId());

        /* Then */
        verify(entry1._2(), never()).pauseDownload();
        verify(entry2._2(), times(1)).pauseDownload();
        verify(entry3._2(), never()).pauseDownload();
        verifyPostLaunchDownload();
    }


    @Test
    public void should_toogle_on_a_PAUSED_download() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();
        entry2._1().setStatus(Status.PAUSED);

        /* When */
        itemDownloadManager.toggleDownload(entry2._1().getId());

        /* Then */
        verify(entry1._2(), never()).restartDownload();
        verify(entry2._2(), times(1)).restartDownload();
        verify(entry3._2(), never()).restartDownload();
        verifyPostLaunchDownload();
    }

    @Test
    public void should_remove_a_current_download() throws URISyntaxException {
        /* Given */
        final Downloader calledDownloader = mock(Downloader.class);
        Item item = new Item().setId(UUID.randomUUID()).setPubDate(ZonedDateTime.now()).setUrl("http://nowhere.else");
        itemDownloadManager.getDownloadingQueue().put(item, calledDownloader);
        /* When */
        itemDownloadManager.removeACurrentDownload(item);
        /* Then */
        assertThat(itemDownloadManager.getDownloadingQueue()).hasSize(0);
        verifyConvertAndSave(times(1));
    }


    @Test
    public void should_move_item_in_queue() {
        /* Given */
        itemDownloadManager.addItemToQueue(ITEM_1);
        itemDownloadManager.addItemToQueue(ITEM_2);
        itemDownloadManager.addItemToQueue(ITEM_3);

        /* When */
        itemDownloadManager.moveItemInQueue(ITEM_2.getId(), 2);

        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue())
                .containsSequence(ITEM_1, ITEM_3, ITEM_2);

        verifyConvertAndSave(times(4));
    }

    @Test(expected = RuntimeException.class)
    public void should_do_nothing_on_non_present_item_movement() {
        /* Given */
        itemDownloadManager.addItemToQueue(ITEM_1);
        itemDownloadManager.addItemToQueue(ITEM_2);
        itemDownloadManager.addItemToQueue(ITEM_3);
        verifyConvertAndSave(times(3));

        /* When */
        itemDownloadManager.moveItemInQueue(UUID.randomUUID(), 2);

        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue()).containsSequence(ITEM_1, ITEM_2, ITEM_3);
    }

    @Test
    public void should_reset_current_download() throws URISyntaxException {
        /* Given */
        when(podcastServerParameters.getNumberOfTry()).thenReturn(3);
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.resetDownload(entry1._1());

        /* Then */
        assertThat(entry1._1().getNumberOfFail()).isEqualTo(1);
        verify(downloaderSelector, times(2)).of(eq(DownloadingItem.builder().item(entry1._1()).urls(List(entry1._1().getUrl())).build()));
        verifyPostLaunchDownload();
    }

    @Test
    public void should_remove_from_both_queue() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(1);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        when(itemRepository.findAllToDownload(any(), eq(5)))
                .thenReturn(HashSet.of(entry1._1()))
                .thenReturn(HashSet.of(entry2._1(), entry3._1()));
        itemDownloadManager.launchDownload();
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.removeItemFromQueueAndDownload(entry1._1());
        itemDownloadManager.removeItemFromQueueAndDownload(entry3._1());

        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue()).hasSize(1);
        verify(entry1._2(), times(1)).stopDownload();
        verifyPostLaunchDownload();
    }

    @Test
    public void should_detect_if_is_in_downloading_queue() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();
                
        /* When */
        Boolean isIn = itemDownloadManager.isInDownloadingQueue(entry1._1());
        Boolean isNotIn = itemDownloadManager.isInDownloadingQueue(Item.DEFAULT_ITEM);

        /* Then */
        assertThat(isIn).isTrue();
        assertThat(isNotIn).isFalse();
        verifyPostLaunchDownload();
    }

    @Test
    public void should_get_downloading_item_list() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        itemDownloadManager.launchDownload();

        /* When */
        Set<Item> items = itemDownloadManager.getItemsInDownloadingQueue();

        /* Then */
        assertThat(items).contains(entry1._1(), entry2._1(), entry3._1());
        verifyPostLaunchDownload();
    }

    @Test
    public void should_find_item_in_downloading_queue() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        itemDownloadManager.launchDownload();

        /* When */
        Item item = itemDownloadManager.getItemInDownloadingQueue(entry1._1().getId());

        /* Then */
        assertThat(item).isSameAs(entry1._1());
        verifyPostLaunchDownload();
    }

    @Test
    public void should_return_null_if_item_not_found_in_downloading_queue() {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        Item item1 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+1).setPubDate(ZonedDateTime.now());
        Item item2 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+2).setPubDate(ZonedDateTime.now());
        Item item3 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+3).setPubDate(ZonedDateTime.now());
        itemDownloadManager.getDownloadingQueue().put(item1, mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(item2, mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(item3, mockDownloader);

        /* When */
        Item item = itemDownloadManager.getItemInDownloadingQueue(UUID.randomUUID());

        /* Then */
        assertThat(item).isNull();
    }

    @Test
    public void should_not_allowed_parallel_update() {
        /* Given */
        /* When */
        itemDownloadManager.setLimitParallelDownload(5);
        runAsync(() -> itemDownloadManager.setLimitParallelDownload(10));

        /* Then */
        await().atMost(5, SECONDS).until(() -> {
            verify(template, atLeast(2)).convertAndSend(eq("/topic/waiting"), any(io.vavr.collection.Queue.class));
        });
    }

    @Test
    public void should_clear_waiting_list() {
        /* Given */
        Tuple2<Item, Downloader> entry1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        Tuple2<Item, Downloader> entry3 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        when(extractorSelector.of(anyString())).thenReturn(new NoOpExtractor());
        when(downloaderExecutor.getCorePoolSize()).thenReturn(1);
        when(podcastServerParameters.getNumberOfTry()).thenReturn(5);
        when(itemRepository.findAllToDownload(any(), eq(5))).thenReturn(HashSet.of(entry1._1(), entry2._1(), entry3._1()));
        itemDownloadManager.launchDownload();

        /* When */
        itemDownloadManager.clearWaitingQueue();

        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue()).hasSize(0);
        verifyPostLaunchDownload();
    }

    @After
    public void afterEach() {
        verify(podcastServerParameters, atLeast(1)).getRootfolder();
        verifyNoMoreInteractions(template, itemRepository, podcastServerParameters, downloaderSelector, extractorSelector);
    }

}
