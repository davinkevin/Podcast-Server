package lan.dk.podcastserver.controller.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/08/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class IDMControllerTest {

    @Mock ItemDownloadManager IDM;
    @InjectMocks IDMController idmController;

    @Test
    public void should_get_download_list() {
        /* Given */
        ConcurrentLinkedQueue<Item> waitingQueue = Queues.newConcurrentLinkedQueue();
        Podcast podcast = Podcast.builder().title("Podcast").build();

        waitingQueue.addAll(Lists.newArrayList(
                Item.builder().title("Foo").podcast(podcast).build(),
                Item.builder().title("Bar").podcast(podcast).build()
        ));
        when(IDM.getWaitingQueue()).thenReturn(waitingQueue);

        /* When */
        Queue<Item> downloadList = idmController.getDownloadList();

        /* Then */
        assertThat(downloadList)
                .hasSize(2)
                .contains(waitingQueue.toArray(new Item[waitingQueue.size()]));
    }
}