package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
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
public class ItemDeletionScheduledTest {

    @Mock UpdatePodcastBusiness updatePodcastBusiness;
    @InjectMocks ItemDeletionScheduled itemDeletionScheduled;

    @Test
    public void should_delete_old_item() {
        /* When */ itemDeletionScheduled.deleteOldItem();
        /* Then */ verify(updatePodcastBusiness, times(1)).deleteOldEpisode();
    }

    @Test
    public void should_delete_old_cover() {
        /* When */ itemDeletionScheduled.deleteOldCover();
        /* Then */ verify(updatePodcastBusiness, times(1)).deleteOldCover();
    }

}
