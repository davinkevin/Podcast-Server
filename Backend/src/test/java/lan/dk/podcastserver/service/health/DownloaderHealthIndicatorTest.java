package lan.dk.podcastserver.service.health;

import io.vavr.collection.Queue;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.UUID;

import static io.vavr.API.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 19/11/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloaderHealthIndicatorTest {

    private @Mock ItemDownloadManager idm;
    private @InjectMocks DownloaderHealthIndicator downloaderHealthIndicator;

    @Test
    public void should_generate_health_information() {
        /* Given */
        Item first = Item.builder()
                .id(UUID.randomUUID())
                .title("first")
                .build();
        Item second = Item.builder()
                .id(UUID.randomUUID())
                .title("second")
                .build();
        Item third = Item.builder()
                .id(UUID.randomUUID())
                .title("third")
                .build();
        Item fourth = Item.builder()
                .id(UUID.randomUUID())
                .title("fourth")
                .build();

        Set<Item> downloadingQueue = Set(first);
        Queue<Item> waitingQueue = Queue.of(second, third, fourth);

        when(idm.getNumberOfCurrentDownload()).thenReturn(1);
        when(idm.getLimitParallelDownload()).thenReturn(3);
        when(idm.getItemsInDownloadingQueue()).thenReturn(downloadingQueue);
        when(idm.getWaitingQueue()).thenReturn(waitingQueue);

        /* When */
        Health health = downloaderHealthIndicator.health();

        /* Then */
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).contains(
                entry("isDownloading", Boolean.TRUE),
                entry("numberOfParallelDownloads", 3),
                entry("numberOfDownloading", downloadingQueue.length()),
                entry("downloadingItems", downloadingQueue),
                entry("numberInQueue", waitingQueue.length()),
                entry("waitingItems", waitingQueue)
        );
    }

}