package lan.dk.podcastserver.controller.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 08/08/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class IDMControllerTest {

    private static final Podcast PODCAST = Podcast.builder().title("Podcast").build();

    @Mock ItemDownloadManager IDM;
    @InjectMocks IDMController idmController;

    @Test
    public void should_get_download_list() {
        /* Given */
        ConcurrentLinkedQueue<Item> waitingQueue = Queues.newConcurrentLinkedQueue();

        waitingQueue.addAll(Lists.newArrayList(
                Item.builder().title("Foo").podcast(PODCAST).build(),
                Item.builder().title("Bar").podcast(PODCAST).build()
        ));
        when(IDM.getWaitingQueue()).thenReturn(waitingQueue);

        /* When */
        Queue<Item> downloadList = idmController.getDownloadList();

        /* Then */
        assertThat(downloadList)
                .hasSize(2)
                .contains(waitingQueue.toArray(new Item[waitingQueue.size()]));
    }

    @Test
    public void should_get_downloading_list() {
        /* Given */
        HashMap<Item, Downloader> downloadingQueue = Maps.newHashMap();
        downloadingQueue.put(Item.builder().id(UUID.randomUUID()).title("Foo").podcast(PODCAST).build(), mock(Downloader.class));
        downloadingQueue.put(Item.builder().id(UUID.randomUUID()).title("Bar").podcast(PODCAST).build(), mock(Downloader.class));
        when(IDM.getDownloadingQueue()).thenReturn(downloadingQueue);

        /* When */
        Set<Item> downloadingList = idmController.getDownloadingList();

        /* Then */
        assertThat(downloadingList)
                .hasSize(2)
                .contains(downloadingQueue.keySet().toArray(new Item[downloadingQueue.size()]));
        verify(IDM, only()).getDownloadingQueue();
    }

    @Test
    public void should_find_item_in_downloading_list_by_id() {
        /* Given */
        Item item = Item.builder().id(UUID.randomUUID()).build();
        when(IDM.getItemInDownloadingQueue(eq(item.getId()))).thenReturn(item);

        /* When */
        Item itemInDownloadingList = idmController.getDownloadingList(item.getId());

        /* Then */
        assertThat(itemInDownloadingList)
                .isSameAs(item)
                .hasId(item.getId());
        verify(IDM, only()).getItemInDownloadingQueue(eq(item.getId()));
    }

    @Test
    public void should_get_number_of_current_download() {
        /* Given */
        when(IDM.getNumberOfCurrentDownload()).thenReturn(6);

        /* When */
        Integer currentD = idmController.getNumberOfCurrentDownload();

        /* Then */
        assertThat(currentD).isEqualTo(6);
    }
}