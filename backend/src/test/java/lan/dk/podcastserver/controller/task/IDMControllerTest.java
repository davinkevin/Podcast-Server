package lan.dk.podcastserver.controller.task;

import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.Podcast;
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager;
import io.vavr.collection.HashSet;
import io.vavr.collection.Queue;
import io.vavr.collection.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/08/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class IDMControllerTest {

    private static final Podcast PODCAST = new Podcast() {{ setTitle("Podcast"); }};

    private @Mock ItemDownloadManager IDM;
    private @InjectMocks IDMController idmController;

    @Test
    public void should_get_download_list() {
        /* Given */
        Item i1 = new Item();
        i1.setTitle("Foo");
        i1.setPodcast(PODCAST);
        Item i2 = new Item();
        i2.setTitle("Bar");
        i2.setPodcast(PODCAST);
        Queue<Item> waitingQueue = Queue.of(i1, i2);
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
        Item i1 = new Item();
        i1.setTitle("Foo");
        i1.setPodcast(PODCAST);
        Item i2 = new Item();
        i2.setTitle("Bar");
        i2.setPodcast(PODCAST);
        Set<Item> items = HashSet.of(i1, i2);
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
        Item item = new Item();
        item.setId(UUID.randomUUID());
        when(IDM.getItemInDownloadingQueue(eq(item.getId()))).thenReturn(item);

        /* When */
        Item itemInDownloadingList = idmController.getDownloadingList(item.getId());

        /* Then */
        assertThat(itemInDownloadingList).isSameAs(item);
        assertThat(itemInDownloadingList.getId()).isEqualTo(item.getId());
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
