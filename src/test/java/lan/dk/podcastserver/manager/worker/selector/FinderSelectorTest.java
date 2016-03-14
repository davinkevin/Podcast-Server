package lan.dk.podcastserver.manager.worker.selector;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.manager.worker.finder.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FinderSelectorTest {

    @Mock RSSFinder rssFinder;
    @Mock YoutubeFinder youtubeFinder;
    @Mock DailymotionFinder dailymotionFinder;
    @Mock PluzzFinder pluzzFinder;
    FinderSelector finderSelector;

    @Before
    public void beforeEach() {
        when(rssFinder.compatibility(anyString())).thenCallRealMethod();
        when(youtubeFinder.compatibility(anyString())).thenCallRealMethod();
        when(dailymotionFinder.compatibility(anyString())).thenCallRealMethod();
        when(pluzzFinder.compatibility(anyString())).thenCallRealMethod();
        finderSelector = new FinderSelector();

        finderSelector.setFinders(Sets.newHashSet(rssFinder, youtubeFinder, dailymotionFinder, pluzzFinder));
    }

    @Test
    public void should_reject_if_url_empty() {
        assertThat(finderSelector.of(null)).isEqualTo(FinderSelector.NO_OP_FINDER);
    }
    
    @Test
    public void should_find_RSS() {
        /* Given */
        String url = "http://foo.bar.com/to/rss/file.xml";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(rssFinder);
    }

    @Test
    public void should_find_youtube() {
        /* Given */
        String url = "http://www.youtube.com/channel/UC_ioajefokjFAOI";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(youtubeFinder);
    }

    @Test
    public void should_find_dailymotion() {
        /* Given */
        String url = "http://www.dailymotion.com/foo/bar";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(dailymotionFinder);
    }

    @Test
    public void should_find_pluzz() {
        /* Given */
        String url = "http://pluzz.francetv.fr/videos/comment_ca_va_bien.html";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(pluzzFinder);
    }
}