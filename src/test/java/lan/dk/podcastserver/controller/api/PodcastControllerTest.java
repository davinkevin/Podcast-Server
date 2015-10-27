package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.business.find.FindPodcastBusiness;
import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

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
        PodcastAssert.assertThat(createdPodcast).isEqualTo(podcast);
        verify(podcastBusiness, only()).create(eq(podcast));
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        Podcast podcast = new Podcast();
        when(podcastBusiness.findOne(anyInt())).thenReturn(podcast);

        /* When */
        Podcast podcastById = podcastController.findById(1);

        /* Then */
        PodcastAssert.assertThat(podcastById).isSameAs(podcast);
        verify(podcastBusiness, only()).findOne(eq(1));
    }

    @Test
    public void should_update() {
        Podcast podcast = new Podcast();
        when(podcastBusiness.reatachAndSave(any(Podcast.class))).thenReturn(podcast);

        /* When */
        Podcast podcastUpdated = podcastController.update(podcast, 1);

        /* Then */
        PodcastAssert.assertThat(podcastUpdated)
                .isInstanceOf(Podcast.class)
                .hasId(1);
        verify(podcastBusiness, only()).reatachAndSave(same(podcast));
    }

    @Test
    public void should_patch_update() {
        /* Given */
        Podcast podcast = new Podcast();
        when(podcastBusiness.patchUpdate(any(Podcast.class))).thenReturn(podcast);

        /* When */
        Podcast podcastUpdated = podcastController.patchUpdate(podcast, 1);

        /* Then */
        PodcastAssert.assertThat(podcastUpdated)
                .isInstanceOf(Podcast.class)
                .hasId(1);
        verify(podcastBusiness, only()).patchUpdate(same(podcast));
    }

    @Test
    public void should_delete() {
        /* Given */
        Integer id = 123;

        /* When */
        podcastController.delete(id);

        /* Then */
        verify(podcastBusiness, only()).delete(eq(id));
    }
}