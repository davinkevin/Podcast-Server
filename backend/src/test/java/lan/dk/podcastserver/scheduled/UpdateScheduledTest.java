package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateScheduledTest {

    @Mock UpdatePodcastBusiness updatePodcastBusiness;
    @Mock ItemDownloadManager IDM;
    @InjectMocks UpdateScheduled updateScheduled;

    @Test
    public void should_update_and_download() {
        /* When */  updateScheduled.updateAndDownloadPodcast();
        /* Then */
        verify(updatePodcastBusiness, times(1)).updatePodcast();
        verify(IDM, times(1)).launchDownload();
    }
}
