package lan.dk.podcastserver.manager.selector;

import lan.dk.podcastserver.manager.worker.Finder;
import lan.dk.podcastserver.manager.worker.beinsports.BeInSportsFinder;
import lan.dk.podcastserver.manager.worker.dailymotion.DailymotionFinder;
import lan.dk.podcastserver.manager.worker.francetv.FranceTvFinder;
import lan.dk.podcastserver.manager.worker.gulli.GulliFinder;
import lan.dk.podcastserver.manager.worker.jeuxvideocom.JeuxVideoComFinder;
import lan.dk.podcastserver.manager.worker.mycanal.MyCanalFinder;
import lan.dk.podcastserver.manager.worker.rss.RSSFinder;
import lan.dk.podcastserver.manager.worker.sixplay.SixPlayFinder;
import lan.dk.podcastserver.manager.worker.tf1replay.TF1ReplayFinder;
import lan.dk.podcastserver.manager.worker.youtube.YoutubeFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.vavr.API.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FinderSelectorTest {

    private @Mock
    BeInSportsFinder beInSportsFinder;
    private @Mock
    MyCanalFinder myCanalFinder;
    private @Mock
    DailymotionFinder dailymotionFinder;
    private @Mock
    FranceTvFinder franceTvFinder;
    private @Mock
    GulliFinder gulliFinder;
    private @Mock
    JeuxVideoComFinder jeuxVideoComFinder;
    private @Mock
    RSSFinder rssFinder;
    private @Mock
    SixPlayFinder sixPlayFinder;
    private @Mock
    TF1ReplayFinder tf1ReplayFinder;
    private @Mock
    YoutubeFinder youtubeFinder;

    private FinderSelector finderSelector;

    @Before
    public void beforeEach() {
        when(beInSportsFinder.compatibility(anyString())).thenCallRealMethod();
        when(myCanalFinder.compatibility(anyString())).thenCallRealMethod();
        when(dailymotionFinder.compatibility(anyString())).thenCallRealMethod();
        when(franceTvFinder.compatibility(anyString())).thenCallRealMethod();
        when(gulliFinder.compatibility(anyString())).thenCallRealMethod();
        when(jeuxVideoComFinder.compatibility(anyString())).thenCallRealMethod();
        when(rssFinder.compatibility(anyString())).thenCallRealMethod();
        when(sixPlayFinder.compatibility(anyString())).thenCallRealMethod();
        when(tf1ReplayFinder.compatibility(anyString())).thenCallRealMethod();
        when(youtubeFinder.compatibility(anyString())).thenCallRealMethod();

        finderSelector = new FinderSelector(Set(beInSportsFinder, myCanalFinder, dailymotionFinder, franceTvFinder, gulliFinder, jeuxVideoComFinder, rssFinder, sixPlayFinder, tf1ReplayFinder, youtubeFinder).toJavaSet());
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
        String url = "http://www.mycanal.fr/c-divertissement/c-le-grand-journal/pid5411-le-grand-journal.html";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(myCanalFinder);
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
    public void should_find_francetv() {
        /* Given */
        String url = "http://www.france.tv/videos/comment_ca_va_bien.html";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(franceTvFinder);
    }

    @Test
    public void should_find_gulli() {
        /* Given */
        String url = "http://replay.gulli.fr/videos/foo/bar";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(gulliFinder);
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
    public void should_find_RSS() {
        /* Given */
        String url = "http://foo.bar.com/to/rss/file.xml";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(rssFinder);
    }

    @Test
    public void should_find_sixplay() {
        /* Given */
        String url = "http://www.6play.fr/videos/foo/bar";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(sixPlayFinder);
    }

    @Test
    public void should_find_tf1replay() {
        /* Given */
        String url = "http://www.tf1.fr/videos/foo/bar";

        /* When */
        Finder finder = finderSelector.of(url);

        /* Then */
        assertThat(finder).isSameAs(tf1ReplayFinder);
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
