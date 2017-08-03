package lan.dk.podcastserver.business.find;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.selector.FinderSelector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 01/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FindPodcastBusinessTest {

    @Mock FinderSelector finderSelector;
    @InjectMocks FindPodcastBusiness findPodcastBusiness;

    @Test
    public void should_not_find_any_finder() {
        /* Given */
        String fakeUrl = "http://any.fake.url/";
        when(finderSelector.of(anyString())).thenReturn(FinderSelector.NO_OP_FINDER);
        /* When */
        Podcast podcast = findPodcastBusiness.fetchPodcastInfoByUrl(fakeUrl);
        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST);
        verify(finderSelector, times(1)).of(eq(fakeUrl));
    }

    @Test
    public void should_find_a_finder() {
         /* Given */
        String fakeUrl = "http://any.fake.url/";
        Finder finder = mock(Finder.class);
        when(finderSelector.of(anyString())).thenReturn(finder);
        when(finder.find(anyString())).thenReturn(new Podcast());

        /* When */
        Podcast podcast = findPodcastBusiness.fetchPodcastInfoByUrl(fakeUrl);

        /* Then */
        assertThat(podcast).isNotNull().isEqualTo(new Podcast());
        verify(finderSelector, times(1)).of(eq(fakeUrl));
        verify(finder, times(1)).find(eq(fakeUrl));
    }
}
