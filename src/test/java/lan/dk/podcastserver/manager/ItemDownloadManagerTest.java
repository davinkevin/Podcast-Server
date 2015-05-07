package lan.dk.podcastserver.manager;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.WorkerService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 06/05/15
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemDownloadManagerTest {

    private static final Integer NUMBER_OF_DOWNLOAD = 3;

    @Captor ArgumentCaptor<String> stringArgumentCaptor;
    @Captor ArgumentCaptor<Queue<Item>> queueArgumentCaptor;

    @Mock SimpMessagingTemplate template;
    @Mock ItemBusiness itemBusiness;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock WorkerService workerService;

    @InjectMocks ItemDownloadManager itemDownloadManager;

    @Test
    public void should_get_limit_of_download () {
        /* Given */ when(podcastServerParameters.concurrentDownload()).thenReturn(NUMBER_OF_DOWNLOAD);
        /* When */ Integer nbOfDownload = itemDownloadManager.getLimitParallelDownload();
        /* Then */
        verify(podcastServerParameters, times(1)).concurrentDownload();
        assertThat(nbOfDownload).isEqualTo(NUMBER_OF_DOWNLOAD);
    }

    @Test
    public void should_change_limit_of_download_sup () {
        /* Given */ when(podcastServerParameters.concurrentDownload()).thenReturn(NUMBER_OF_DOWNLOAD);
        /* When */ itemDownloadManager.changeLimitParallelsDownload(NUMBER_OF_DOWNLOAD + 1);
        /* Then */ verify(podcastServerParameters, times(1)).concurrentDownload();
    }

    @Test
    public void should_change_limit_of_download_less () {
        /* Given */ when(podcastServerParameters.concurrentDownload()).thenReturn(NUMBER_OF_DOWNLOAD);
        /* When */  itemDownloadManager.changeLimitParallelsDownload(NUMBER_OF_DOWNLOAD - 1);
        /* Then */
        verify(podcastServerParameters, times(1)).concurrentDownload();
        verify(template, times(1)).convertAndSend(stringArgumentCaptor.capture(), queueArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualTo("/topic/waiting");
        assertThat(queueArgumentCaptor.getValue()).isNotNull().isEmpty();

    }

    @Test
    public void should_get_empty_waiting_queue () {
        assertThat(itemDownloadManager.getWaitingQueue()).isNotNull().isEmpty();
    }

    @Test
    public void should_get_empty_downloading_queue () {
        assertThat(itemDownloadManager.getDownloadingQueue()).isNotNull().isEmpty();
    }

    @Test
    public void should_get_number_of_current_download () {
        assertThat(itemDownloadManager.getNumberOfCurrentDownload()).isEqualTo(0);
    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(template, itemBusiness, podcastServerParameters, workerService);
    }



}