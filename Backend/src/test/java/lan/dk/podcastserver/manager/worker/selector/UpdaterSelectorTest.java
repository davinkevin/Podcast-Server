package lan.dk.podcastserver.manager.worker.selector;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.manager.worker.updater.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdaterSelectorTest {

    private UpdaterSelector updaterSelector;

    private @Mock BeInSportsUpdater beInSportsUpdater;
    private @Mock CanalPlusUpdater canalPlusUpdater;
    private @Mock DailymotionUpdater dailymotionUpdater;
    private @Mock GulliUpdater gulliUpdater;
    private @Mock JeuxVideoComUpdater jeuxVideoComUpdater;
    private @Mock FranceTvUpdater franceTvUpdater;
    private @Mock RSSUpdater rssUpdater;
    private @Mock SixPlayUpdater sixPlayUpdater;
    private @Mock TF1ReplayUpdater tf1ReplayUpdater;
    private @Mock UploadUpdater uploadUpdater;
    private @Mock YoutubeUpdater youtubeUpdater;

    @Before
    public void setUp() throws Exception {
        /* Given */
        HashSet<Updater> updaters = HashSet.of(
                beInSportsUpdater, canalPlusUpdater, dailymotionUpdater, gulliUpdater,
                jeuxVideoComUpdater, franceTvUpdater, rssUpdater, sixPlayUpdater,
                tf1ReplayUpdater, uploadUpdater, youtubeUpdater
        );

        updaters.forEach(u -> {
            when(u.compatibility(anyString())).thenCallRealMethod();
            when(u.type()).thenCallRealMethod();
        });

        updaterSelector = new UpdaterSelector(updaters.toJavaSet());
    }


    @Test
    public void should_return_a_BeInSportUpdater () {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.beinsports.com/replay/category/3361/name/lexpresso");
        /* Then */ assertThat(updaterClass).isEqualTo(beInSportsUpdater);
    }

    @Test
    public void should_return_a_CanalPlusUpdater() {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.canalplus.fr/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(canalPlusUpdater);
    }

    @Test
    public void should_return_a_DailymotionUpdater() {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.dailymotion.com/showname");
        /* Then */ assertThat(updaterClass).isEqualTo(dailymotionUpdater);
    }

    @Test
    public void should_return_a_GulliUpdater() {
        /* When */ Updater updaterClass = updaterSelector.of("http://replay.gulli.fr/showname");
        /* Then */ assertThat(updaterClass).isEqualTo(gulliUpdater);
    }

    @Test
    public void should_return_a_JeuxVideoComUpdater() {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.jeuxvideo.com/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(jeuxVideoComUpdater);
    }

    @Test
    public void should_return_a_FranceTvUpdate() {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.france.tv/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(franceTvUpdater);
    }

    @Test
    public void should_return_an_RssUpdater () {
        /* When */ Updater updaterClass = updaterSelector.of("www.link.to.rss/feeds");
        /* Then */ assertThat(updaterClass).isEqualTo(rssUpdater);
    }

    @Test
    public void should_return_an_SixPlayUpdater () {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.6play.fr/turbo_test");
        /* Then */ assertThat(updaterClass).isEqualTo(sixPlayUpdater);
    }

    @Test
    public void should_return_an_Tf1ReplayUpdater () {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.tf1.fr/title");
        /* Then */ assertThat(updaterClass).isEqualTo(tf1ReplayUpdater);
    }

    @Test
    public void should_return_a_YoutubeUpdater () {
        /* When */ Updater updaterClass = updaterSelector.of("http://www.youtube.com/user/fakeUser");
        /* Then */ assertThat(updaterClass).isEqualTo(youtubeUpdater);
    }

    @Test
    public void should_reject_empty_url() {
        /* When */
        assertThat(updaterSelector.of("")).isEqualTo(UpdaterSelector.NO_OP_UPDATER);
    }

    @Test
    public void should_serve_types() {
        /* When */
        Set<AbstractUpdater.Type> types = updaterSelector.types();

        /* Then */
        assertThat(types).isNotEmpty().hasSize(11);
    }

}
