package lan.dk.podcastserver.manager;

import com.google.common.collect.Lists;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.downloader.NoOpDownloader;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 06/05/15
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemDownloadManagerTest {

    private static final Integer NUMBER_OF_DOWNLOAD = 3;
    private static final Path ROOT_FOLDER = Paths.get("/tmp/ps");

    @Captor ArgumentCaptor<String> stringArgumentCaptor;
    @Captor ArgumentCaptor<Queue<Item>> queueArgumentCaptor;
    @Captor ArgumentCaptor<String> itemUrlArgumentCaptor;
    @Captor ArgumentCaptor<Item> itemArgumentCaptor;
    @Captor ArgumentCaptor<UUID> integerArgumentCaptor;

    @Mock SimpMessagingTemplate template;
    @Mock ItemRepository itemRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock DownloaderSelector downloaderSelector;
    @Mock ThreadPoolTaskExecutor downloaderExecutor;

    @InjectMocks ItemDownloadManager itemDownloadManager;

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
    public void should_get_root_folder() {
        /* Given */ when(podcastServerParameters.getRootfolder()).thenReturn(ROOT_FOLDER);
        /* When */ String rootfolder = itemDownloadManager.getRootfolder();
        /* Then */
        verify(podcastServerParameters, times(2)).getRootfolder();
        assertThat(rootfolder).isSameAs(ROOT_FOLDER.toString());
    }

    @Test
    public void should_init_download_with_empty_list() throws URISyntaxException {
        /* Given */
        when(itemRepository.findAllToDownload(any())).thenReturn(new ArrayList<>());
        /* When */
        itemDownloadManager.launchDownload();
        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate();
        verify(itemRepository, times(1)).findAllToDownload(any());
        verify(template, times(1)).convertAndSend(stringArgumentCaptor.capture(), queueArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualTo("/topic/waiting");
    }

    @Test
    public void should_init_download_with_list_of_item_larger_than_download_limit() throws URISyntaxException {
        /* Given */
        final List<Item> itemList = Lists.newArrayList(
                Item.builder().id(UUID.randomUUID()).url("1").status(Status.NOT_DOWNLOADED).build(),
                Item.builder().id(UUID.randomUUID()).url("2").status(Status.NOT_DOWNLOADED).build(),
                Item.builder().id(UUID.randomUUID()).url("3").status(Status.NOT_DOWNLOADED).build(),
                Item.builder().id(UUID.randomUUID()).url("4").status(Status.NOT_DOWNLOADED).build()
        );
        when(itemRepository.findAllToDownload(any())).thenReturn(itemList);
        Downloader downloader = mock(Downloader.class);
        when(downloaderSelector.of(anyString())).thenReturn(downloader);
        when(downloader.setItem(any())).thenReturn(downloader);
        when(downloader.setItemDownloadManager(any())).thenReturn(downloader);
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);

        /* When */
        itemDownloadManager.launchDownload();

        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate();
        verify(itemRepository, times(1)).findAllToDownload(any());
        verify(downloaderSelector, times(3)).of(itemUrlArgumentCaptor.capture());
        assertThat(itemUrlArgumentCaptor.getAllValues()).contains(itemList.get(0).getUrl(), itemList.get(1).getUrl(), itemList.get(2).getUrl());
        verify(template, times(1)).convertAndSend(eq("/topic/waiting"), same(itemDownloadManager.getWaitingQueue()));
    }

    @Test
    public void should_relaunch_a_paused_download() throws URISyntaxException {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        final Item item = Item.builder().id(UUID.randomUUID()).status(Status.NOT_DOWNLOADED).build();
        itemDownloadManager.getDownloadingQueue().put(item, mockDownloader);
        when(itemRepository.findAllToDownload(any())).thenReturn(Collections.singletonList(item));

        /* When */
        itemDownloadManager.launchDownload();

        /* Then */
        verify(podcastServerParameters, times(1)).limitDownloadDate();
        verify(itemRepository, times(1)).findAllToDownload(any());
        verifyConvertAndSave();
    }

    @Test
    public void should_stop_all_downloads() {
        /* Given */
        final Downloader mockDownloader1 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        final Downloader mockDownloader2 = generateDownloaderAndRegisterIt(UUID.randomUUID());
        final Downloader mockDownloader3 = generateDownloaderAndRegisterIt(UUID.randomUUID());

        /* When */
        itemDownloadManager.stopAllDownload();

        /* Then */
        verify(mockDownloader1, times(1)).stopDownload();
        verify(mockDownloader2, times(1)).stopDownload();
        verify(mockDownloader3, times(1)).stopDownload();
    }

    private Downloader generateDownloaderAndRegisterIt(UUID id) {
        final Downloader mockDownloader = mock(Downloader.class);
        Item item = new Item().setId(id);
        when(mockDownloader.getItem()).thenReturn(item);
        itemDownloadManager.getDownloadingQueue().put(item, mockDownloader);
        return mockDownloader;
    }

    @Test
    public void should_pause_all_downloads() {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        itemDownloadManager.getDownloadingQueue().put(new Item().setId(UUID.randomUUID()), mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(new Item().setId(UUID.randomUUID()), mockDownloader);

        /* When */
        itemDownloadManager.pauseAllDownload();

        /* Then */
        verify(mockDownloader, times(2)).pauseDownload();
    }

    @Test
    public void should_restart_all_downloads() {
        /* Given */
        final Item itemOne = new Item().setId(UUID.randomUUID()).setStatus(Status.PAUSED);
        final Item itemTwo = new Item().setId(UUID.randomUUID()).setStatus(Status.PAUSED);
        final Downloader mockDownloaderItemOne = mock(Downloader.class);
        final Downloader mockDownloaderItemTwo = mock(Downloader.class);
        when(mockDownloaderItemOne.getItem()).thenReturn(itemOne);
        when(mockDownloaderItemTwo.getItem()).thenReturn(itemTwo);
        itemDownloadManager.getDownloadingQueue().put(itemOne, mockDownloaderItemOne);
        itemDownloadManager.getDownloadingQueue().put(itemTwo, mockDownloaderItemTwo);

        /* When */
        itemDownloadManager.restartAllDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            verify(mockDownloaderItemOne, times(2)).getItem();
            verify(mockDownloaderItemTwo, times(2)).getItem();
            verify(mockDownloaderItemOne, times(1)).startDownload();
            verify(mockDownloaderItemTwo, times(1)).startDownload();
        });
    }

    @Test
    public void should_add_item_to_queue() throws URISyntaxException {
        /* Given */
        Item item = Item.builder().id(UUID.randomUUID()).status(Status.FINISH).build();
        when(itemRepository.findOne(any(UUID.class))).thenReturn(item);
        when(downloaderExecutor.getCorePoolSize()).thenReturn(3);

        /* When */
        itemDownloadManager.addItemToQueue(item.getId());

        /* Then */
        verifyConvertAndSave();
        verify(itemRepository, times(1)).findOne(eq(item.getId()));
        assertThat(itemDownloadManager.getWaitingQueue()).isEmpty();
    }

    @Test
    public void should_not_treat_item_in_waiting_list() {
        /* Given */
        final Item item = new Item().setId(UUID.randomUUID());
        itemDownloadManager.getWaitingQueue().add(item);

        /* When */ itemDownloadManager.addItemToQueue(item);
    }

    @Test
    public void should_remove_from_queue() {
        /* Given */
        final Item item = new Item().setId(UUID.randomUUID());
        when(itemRepository.findOne(any(UUID.class))).thenReturn(item);

        /* When */ itemDownloadManager.removeItemFromQueue(item.getId(), true);
        /* Then */
        verify(itemRepository, times(1)).findOne(integerArgumentCaptor.capture());
        assertThat(integerArgumentCaptor.getValue()).isEqualTo(item.getId());
        verify(itemRepository, times(1)).save(eq(item));
        verifyConvertAndSave();
    }

    private void verifyConvertAndSave() {
        verify(template, times(1)).convertAndSend(stringArgumentCaptor.capture(), queueArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualTo("/topic/waiting");
        assertThat(queueArgumentCaptor.getValue()).isSameAs(itemDownloadManager.getWaitingQueue());
    }

    @Test
    public void should_check_if_can_be_reseted () {
        /* Given */ when(podcastServerParameters.getNumberOfTry()).thenReturn(3);
        /* When */
        Boolean isResetable = itemDownloadManager.canBeReseted(new Item().setNumberOfTry(2));
        Boolean isNotResetable = itemDownloadManager.canBeReseted(new Item().setNumberOfTry(4));

        /* Then */
        assertThat(isResetable).isTrue();
        assertThat(isNotResetable).isFalse();
        verify(podcastServerParameters, times(2)).getNumberOfTry();

    }

    @Test
    public void should_stop_a_current_download() {
        /* Given */
        final Downloader notCalledDownloader = mock(Downloader.class);
        final Downloader calledDownloader = mock(Downloader.class);
        itemDownloadManager.getDownloadingQueue().put(ITEM_1, notCalledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_2, calledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_3, notCalledDownloader);

        /* When */ itemDownloadManager.stopDownload(ITEM_2.getId());

        /* Then */
        verify(calledDownloader, times(1)).stopDownload();
        verifyNoMoreInteractions(notCalledDownloader, calledDownloader);
    }

    @Test
    public void should_pause_a_current_download() {
        /* Given */
        final Downloader notCalledDownloader = mock(Downloader.class);
        final Downloader calledDownloader = mock(Downloader.class);
        itemDownloadManager.getDownloadingQueue().put(ITEM_1, notCalledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_2, calledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_3, notCalledDownloader);

        /* When */ itemDownloadManager.pauseDownload(ITEM_2.getId());

        /* Then */
        verify(calledDownloader, times(1)).pauseDownload();
        verifyNoMoreInteractions(notCalledDownloader, calledDownloader);
    }

    @Test
    public void should_restart_a_current_download() {
        /* Given */
        final Downloader notCalledDownloader = mock(Downloader.class);
        final Downloader calledDownloader = mock(Downloader.class);
        itemDownloadManager.getDownloadingQueue().put(ITEM_1, notCalledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_2, calledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_3, notCalledDownloader);
        when(calledDownloader.getItem()).thenReturn(ITEM_2);


        /* When */ itemDownloadManager.restartDownload(ITEM_2.getId());

        /* Then */
        verify(calledDownloader, times(1)).startDownload();
        verify(calledDownloader, times(1)).getItem();
        verifyNoMoreInteractions(notCalledDownloader, calledDownloader);
    }

    @Test
    public void should_toogle_on_a_STARTED_download() {
        /* Given */
        final Downloader notCalledDownloader = mock(Downloader.class);
        final Downloader calledDownloader = mock(Downloader.class);
        itemDownloadManager.getDownloadingQueue().put(ITEM_1, notCalledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_2, calledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_3, notCalledDownloader);
        when(calledDownloader.getItem()).thenReturn(ITEM_2);

        /* When */ itemDownloadManager.toogleDownload(ITEM_2.getId());

        /* Then */
        verify(calledDownloader, times(1)).pauseDownload();
        verify(calledDownloader, times(1)).getItem();
        verifyNoMoreInteractions(notCalledDownloader, calledDownloader);
    }

    @Test
    public void should_toogle_on_a_PAUSED_download() {
        /* Given */
        final Downloader notCalledDownloader = mock(Downloader.class);
        final Downloader calledDownloader = mock(Downloader.class);
        itemDownloadManager.getDownloadingQueue().put(ITEM_1, notCalledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_2, notCalledDownloader);
        itemDownloadManager.getDownloadingQueue().put(ITEM_3, calledDownloader);
        when(calledDownloader.getItem()).thenReturn(ITEM_3);

        /* When */ itemDownloadManager.toogleDownload(ITEM_3.getId());

        /* Then */
        verify(calledDownloader, times(2)).getItem();
        verify(calledDownloader, times(1)).startDownload();
        verifyNoMoreInteractions(notCalledDownloader, calledDownloader);
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
        verifyConvertAndSave();
    }


    @Test
    public void should_move_item_in_queue() {
        /* Given */
        itemDownloadManager.getWaitingQueue().add(ITEM_1);
        itemDownloadManager.getWaitingQueue().add(ITEM_2);
        itemDownloadManager.getWaitingQueue().add(ITEM_3);
        /* When */ itemDownloadManager.moveItemInQueue(ITEM_2.getId(), 2);
        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue())
                .containsSequence(ITEM_1, ITEM_3, ITEM_2);

        verifyConvertAndSave();
    }

    @Test
    public void should_do_nothing_on_non_present_item_movement() {
        itemDownloadManager.getWaitingQueue().add(ITEM_1);
        itemDownloadManager.getWaitingQueue().add(ITEM_2);
        itemDownloadManager.getWaitingQueue().add(ITEM_3);
        /* When */ itemDownloadManager.moveItemInQueue(UUID.randomUUID(), 2);
        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue())
                .containsSequence(ITEM_1, ITEM_2, ITEM_3);
    }

    @Test
    public void should_reset_current_download() throws URISyntaxException {
        /* Given */
        when(podcastServerParameters.getNumberOfTry()).thenReturn(3);
        Downloader downloaderMock = mock(Downloader.class);
        when(downloaderSelector.of(anyString())).thenReturn(downloaderMock);
        final Downloader calledDownloader = mock(Downloader.class);
        when(downloaderMock.setItem(any())).thenReturn(downloaderMock);
        when(downloaderMock.setItemDownloadManager(any())).thenReturn(downloaderMock);
        Item item = new Item().setId(UUID.randomUUID()).setUrl("1").setPubDate(ZonedDateTime.now()).setUrl("http://nowhere.else");
        itemDownloadManager.getDownloadingQueue().put(item, calledDownloader);

        /* When */ itemDownloadManager.resetDownload(item);
        /* Then */
        assertThat(item.getNumberOfTry()).isEqualTo(1);
        verify(podcastServerParameters, times(1)).getNumberOfTry();
        verify(downloaderSelector, times(1)).of(eq(item.getUrl()));
    }

    @Test
    public void should_remove_from_both_queue() {
        /* Given */
        final Downloader calledDownloader = mock(Downloader.class);
        Item itemWaiting = new Item().setId(UUID.randomUUID()).setPubDate(ZonedDateTime.now()).setUrl("http://nowhere.else");
        Item itemDownloading = new Item().setId(UUID.randomUUID()).setPubDate(ZonedDateTime.now()).setUrl("http://nowhere.else");
        itemDownloadManager.getDownloadingQueue().put(itemDownloading, calledDownloader);
        itemDownloadManager.getWaitingQueue().add(itemWaiting);
        /* When */
        itemDownloadManager.removeItemFromQueueAndDownload(itemWaiting);
        itemDownloadManager.removeItemFromQueueAndDownload(itemDownloading);

        /* Then */
        assertThat(itemDownloadManager.getWaitingQueue()).isEmpty();
        verify(calledDownloader, times(1)).stopDownload();
        verify(template, times(2)).convertAndSend(stringArgumentCaptor.capture(), queueArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualTo("/topic/waiting");
        assertThat(queueArgumentCaptor.getValue()).isSameAs(itemDownloadManager.getWaitingQueue());
    }

    @Test
    public void should_detect_if_is_in_downloading_queue() {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        final Item item = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/").setPubDate(ZonedDateTime.now());
        itemDownloadManager.getDownloadingQueue().put(item, mockDownloader);
                
        /* When */
        Boolean isIn = itemDownloadManager.isInDownloadingQueue(item);
        Boolean isNotIn = itemDownloadManager.isInDownloadingQueue(new Item());

        /* Then */
        assertThat(isIn).isTrue();
        assertThat(isNotIn).isFalse();
    }

    @Test
    public void should_get_downloading_item_list() {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        Item item1 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+1).setPubDate(ZonedDateTime.now());
        Item item2 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+2).setPubDate(ZonedDateTime.now());
        Item item3 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+3).setPubDate(ZonedDateTime.now());
        itemDownloadManager.getDownloadingQueue().put(item1, mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(item2, mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(item3, mockDownloader);

        /* When */ Set<Item> items = itemDownloadManager.getItemsInDownloadingQueue();

        /* Then */
        assertThat(items).contains(item1, item2, item3);
    }

    @Test
    public void should_find_item_in_downloading_queue() {
        /* Given */
        final Downloader mockDownloader = mock(Downloader.class);
        Item item1 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+1).setPubDate(ZonedDateTime.now());
        Item item2 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+2).setPubDate(ZonedDateTime.now());
        Item item3 = new Item().setId(UUID.randomUUID()).setStatus(Status.NOT_DOWNLOADED).setUrl("http://now.where/"+3).setPubDate(ZonedDateTime.now());
        itemDownloadManager.getDownloadingQueue().put(item1, mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(item2, mockDownloader);
        itemDownloadManager.getDownloadingQueue().put(item3, mockDownloader);

        /* When */
        Item item = itemDownloadManager.getItemInDownloadingQueue(item2.getId());

        /* Then */
        assertThat(item).isSameAs(item2);
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
        when(downloaderSelector.of(any())).then(i -> {
            TimeUnit.SECONDS.sleep(1);
            return new NoOpDownloader();
        });

        /* When */
        itemDownloadManager.setLimitParallelDownload(5);
        runAsync(() -> itemDownloadManager.setLimitParallelDownload(10));

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            verify(template, atLeast(2)).convertAndSend(eq("/topic/waiting"), anyCollection());
        });
    }

    @After
    public void afterEach() {
        verify(podcastServerParameters, atLeast(1)).getRootfolder();
        verifyNoMoreInteractions(template, itemRepository, podcastServerParameters, downloaderSelector);
    }

}