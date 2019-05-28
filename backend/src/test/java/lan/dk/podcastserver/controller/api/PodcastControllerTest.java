package lan.dk.podcastserver.controller.api;

import com.github.davinkevin.podcastserver.business.PodcastBusiness;
import com.github.davinkevin.podcastserver.business.find.FindPodcastBusiness;
import com.github.davinkevin.podcastserver.entity.Podcast;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.anyString;
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
    @InjectMocks PodcastController podcastController;

    @Test
    public void should_patch_update() {
        /* Given */
        Podcast podcast = new Podcast();
        UUID id = UUID.randomUUID();
        when(podcastBusiness.patchUpdate(any(Podcast.class))).thenReturn(podcast);

        /* When */
        Podcast podcastUpdated = podcastController.patchUpdate(podcast, id);

        /* Then */
        assertThat(podcastUpdated).isInstanceOf(Podcast.class);
        assertThat(podcastUpdated.getId()).isEqualTo(id);
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

//    @Test
//    public void should_get_rss_with_origin_header() {
//        /* Given */
//        UUID id = UUID.randomUUID();
//        Boolean limit = Boolean.TRUE;
//        HttpServletRequest resquest = mock(HttpServletRequest.class);
//        when(resquest.getHeader(eq("origin"))).thenReturn("http://localhost");
//        when(podcastBusiness.getRss(any(UUID.class), anyBoolean(), anyString())).thenReturn("Foo");
//
//        /* When */
//        String rss = podcastController.getRss(id, limit, resquest);
//
//        /* Then */
//        assertThat(rss).isEqualTo("Foo");
//        verify(podcastBusiness, only()).getRss(eq(id), eq(limit), eq("http://localhost"));
//    }

//    @Test
//    public void should_get_rss_without_origin_header() {
//        /* Given */
//        UUID id = UUID.randomUUID();
//        Boolean limit = Boolean.TRUE;
//        HttpServletRequest resquest = mock(HttpServletRequest.class);
//        when(resquest.getHeader(eq("origin"))).thenReturn(null);
//        when(resquest.getScheme()).thenReturn("http");
//        when(resquest.getServerName()).thenReturn("localhost");
//        when(resquest.getServerPort()).thenReturn(6060);
//
//        when(podcastBusiness.getRss(any(UUID.class), anyBoolean(), anyString())).then(i -> "Foo");
//
//        /* When */
//        String rss = podcastController.getRss(id, limit, resquest);
//
//        /* Then */
//        assertThat(rss).isEqualTo("Foo");
//        verify(podcastBusiness, only()).getRss(eq(id), eq(limit), eq("http://localhost:6060"));
//    }

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
