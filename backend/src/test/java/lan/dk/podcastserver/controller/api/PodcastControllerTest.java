package lan.dk.podcastserver.controller.api;

import com.github.davinkevin.podcastserver.business.PodcastBusiness;
import com.github.davinkevin.podcastserver.business.find.FindPodcastBusiness;
import com.github.davinkevin.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
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
        assertThat(createdPodcast).isEqualTo(podcast);
        verify(podcastBusiness, only()).create(eq(podcast));
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        Podcast podcast = new Podcast();
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);
        UUID id = UUID.randomUUID();

        /* When */
        Podcast podcastById = podcastController.findById(id);

        /* Then */
        assertThat(podcastById).isSameAs(podcast);
        verify(podcastBusiness, only()).findOne(eq(id));
    }

    @Test
    public void should_update() {
        Podcast podcast = new Podcast();
        when(podcastBusiness.reatachAndSave(any(Podcast.class))).thenReturn(podcast);
        UUID id = UUID.randomUUID();

        /* When */
        Podcast podcastUpdated = podcastController.update(podcast, id);

        /* Then */
        assertThat(podcastUpdated)
                .isInstanceOf(Podcast.class)
                .hasId(id);
        verify(podcastBusiness, only()).reatachAndSave(same(podcast));
    }

    @Test
    public void should_patch_update() {
        /* Given */
        Podcast podcast = new Podcast();
        UUID id = UUID.randomUUID();
        when(podcastBusiness.patchUpdate(any(Podcast.class))).thenReturn(podcast);

        /* When */
        Podcast podcastUpdated = podcastController.patchUpdate(podcast, id);

        /* Then */
        assertThat(podcastUpdated)
                .isInstanceOf(Podcast.class)
                .hasId(id);
        verify(podcastBusiness, only()).patchUpdate(same(podcast));
    }

    @Test
    public void should_delete() {
        /* Given */
        UUID id = UUID.randomUUID();

        /* When */
        podcastController.delete(id);

        /* Then */
        verify(podcastBusiness, only()).delete(eq(id));
    }

    @Test
    public void should_find_all() {
        /* Given */
        List<Podcast> podcasts = new ArrayList<>();
        when(podcastBusiness.findAll()).thenReturn(podcasts);

        /* When */
        List<Podcast> all = podcastController.findAll();

        /* Then */
        assertThat(podcasts).isSameAs(all);
    }

    @Test
    public void should_get_rss_with_origin_header() {
        /* Given */
        UUID id = UUID.randomUUID();
        Boolean limit = Boolean.TRUE;
        HttpServletRequest resquest = mock(HttpServletRequest.class);
        when(resquest.getHeader(eq("origin"))).thenReturn("http://localhost");
        when(podcastBusiness.getRss(any(UUID.class), anyBoolean(), anyString())).thenReturn("Foo");

        /* When */
        String rss = podcastController.getRss(id, limit, resquest);

        /* Then */
        assertThat(rss).isEqualTo("Foo");
        verify(podcastBusiness, only()).getRss(eq(id), eq(limit), eq("http://localhost"));
    }

    @Test
    public void should_get_rss_without_origin_header() {
        /* Given */
        UUID id = UUID.randomUUID();
        Boolean limit = Boolean.TRUE;
        HttpServletRequest resquest = mock(HttpServletRequest.class);
        when(resquest.getHeader(eq("origin"))).thenReturn(null);
        when(resquest.getScheme()).thenReturn("http");
        when(resquest.getServerName()).thenReturn("localhost");
        when(resquest.getServerPort()).thenReturn(6060);

        when(podcastBusiness.getRss(any(UUID.class), anyBoolean(), anyString())).then(i -> "Foo");

        /* When */
        String rss = podcastController.getRss(id, limit, resquest);

        /* Then */
        assertThat(rss).isEqualTo("Foo");
        verify(podcastBusiness, only()).getRss(eq(id), eq(limit), eq("http://localhost:6060"));
    }

    @Test
    public void should_fetch_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        when(findPodcastBusiness.fetchPodcastInfoByUrl(anyString())).thenReturn(podcast);

        /* When */
        Podcast anUrl = podcastController.fetchPodcastInfoByUrl("anUrl");

        /* Then */
        assertThat(anUrl).isSameAs(podcast);
    }
}
