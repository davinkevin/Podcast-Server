package lan.dk.podcastserver.controller.task;

import io.vavr.collection.HashSet;
import io.vavr.collection.Queue;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 08/08/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class IDMControllerTest {

    private static final Podcast PODCAST = Podcast.builder().title("Podcast").build();

    private @Mock ItemDownloadManager IDM;
    private @InjectMocks IDMController idmController;

    @Test
    public void should_get_download_list() {
        /* Given */
        Queue<Item> waitingQueue = Queue.of(
                Item.builder().title("Foo").podcast(PODCAST).build(),
                Item.builder().title("Bar").podcast(PODCAST).build()
        );
        when(IDM.getWaitingQueue()).thenReturn(waitingQueue);

        /* When */
        Queue<Item> downloadList = idmController.getDownloadList();

        /* Then */
        assertThat(downloadList)
                .hasSize(2)
                .contains(waitingQueue.toJavaArray(Item.class));
    }

    @Test
    public void should_get_downloading_list() {
        /* Given */
        Set<Item> items = HashSet.of(
                Item.builder().id(UUID.randomUUID()).title("Foo").podcast(PODCAST).build(),
                Item.builder().id(UUID.randomUUID()).title("Bar").podcast(PODCAST).build()
        );
        when(IDM.getItemsInDownloadingQueue()).thenReturn(items);

        /* When */
        Set<Item> downloadingList = idmController.getDownloadingList();

        /* Then */
        assertThat(downloadingList).hasSize(2).containsAll(items);
        verify(IDM, only()).getItemsInDownloadingQueue();
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

    @Test
    public void should_get_limit_of_parallel_download() {
        /* Given */
        when(IDM.getLimitParallelDownload()).thenReturn(3);
        /* When */
        Integer limit = idmController.getLimitParallelDownload();
        /* Then */
        assertThat(limit).isEqualTo(3);
        verify(IDM, only()).getLimitParallelDownload();
    }

    @Test
    public void should_set_limit_of_parallel_download() {
        /* Given */
        /* When */
        idmController.setLimitParallelDownload(3);

        /* Then */
        verify(IDM, only()).setLimitParallelDownload(eq(3));
    }

    @Test
    public void should_launch_download() {
        /* Given */
        /* When */
        idmController.launchDownload();
        /* Then */
        verify(IDM, only()).launchDownload();
    }

}
