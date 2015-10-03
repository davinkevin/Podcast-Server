package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.business.find.FindPodcastBusiness;
import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static lan.dk.podcastserver.entity.PodcastAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 30/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class PodcastControllerTest {

    @Mock PodcastBusiness podcastBusiness;
    @Mock FindPodcastBusiness findPodcastBusiness;
    @Mock StatsBusiness statsBusiness;
    @InjectMocks PodcastController podcastController;

    @Test
    public void should_create_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        when(podcastBusiness.create(eq(podcast))).thenReturn(podcast);

        /* When */
        Podcast createdPodcast = podcastController.create(podcast);

        /* Then */
        assertThat(createdPodcast).isEqualTo(podcast);
        verify(podcastBusiness, only()).create(eq(podcast));
    }
    
}