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

    @Mock BeInSportsFinder beInSportsFinder;
    @Mock CanalPlusFinder canalPlusFinder;
    @Mock DailymotionFinder dailymotionFinder;
    @Mock JeuxVideoComFinder jeuxVideoComFinder;
    @Mock PluzzFinder pluzzFinder;
    @Mock RSSFinder rssFinder;
    @Mock YoutubeFinder youtubeFinder;

    private FinderSelector finderSelector;

    @Before
    public void beforeEach() {
        when(beInSportsFinder.compatibility(anyString())).thenCallRealMethod();
        when(canalPlusFinder.compatibility(anyString())).thenCallRealMethod();
        when(dailymotionFinder.compatibility(anyString())).thenCallRealMethod();
        when(jeuxVideoComFinder.compatibility(anyString())).thenCallRealMethod();
        when(pluzzFinder.compatibility(anyString())).thenCallRealMethod();
        when(rssFinder.compatibility(anyString())).thenCallRealMethod();
        when(youtubeFinder.compatibility(anyString())).thenCallRealMethod();

        finderSelector = new FinderSelector(Sets.newHashSet(beInSportsFinder, canalPlusFinder, dailymotionFinder, jeuxVideoComFinder, pluzzFinder, rssFinder, youtubeFinder));
    }

    @Test
    public void should_reject_if_url_empty() {
        assertThat(finderSelector.of(null)).isEqualTo(FinderSelector.NO_OP_FINDER);
    }


    @Test
    public void should_find_beinsports() {
        /* Given */
        String url = "http://www.beinsports.com/france/replay/lexpresso";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(beInSportsFinder);
    }

    @Test
    public void should_find_canalplus() {
        /* Given */
        String url = "http://www.canalplus.fr/c-divertissement/c-le-grand-journal/pid5411-le-grand-journal.html";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(canalPlusFinder);
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
    public void should_find_jeuxvideocom() {
        /* Given */
        String url = "http://www.jeuxvideo.com/chroniques-video.htm";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(jeuxVideoComFinder);
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
}