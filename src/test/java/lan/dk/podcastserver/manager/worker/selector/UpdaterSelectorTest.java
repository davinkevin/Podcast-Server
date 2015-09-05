package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.selector.update.*;
import lan.dk.podcastserver.manager.worker.updater.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdaterSelectorTest {

    Set<UpdaterCompatibility> updaterSelectors = new HashSet<>();
    
    @Before
    public void setUp() throws Exception {
        updaterSelectors.add(new YoutubeUpdaterCompatibility());
        updaterSelectors.add(new RssUpdaterCompatibility());
        updaterSelectors.add(new BeInSportUpdaterCompatibility());
        updaterSelectors.add(new CanalPlusUpdaterCompatibility());
        updaterSelectors.add(new JeuxVideoFrCompatibility());
        updaterSelectors.add(new JeuxVideoComCompatibility());
        updaterSelectors.add(new ParleysCompatibility());
        updaterSelectors.add(new PluzzCompatibility());
    }
    
    @Test
    public void should_return_an_RssUpdater () {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("www.link.to.rss/feeds");
        /* Then */ assertThat(updaterClass).isEqualTo(RSSUpdater.class);
    }
    
    @Test
    public void should_return_a_YoutubeUpdater () {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.youtube.com/user/fakeUser");
        /* Then */ assertThat(updaterClass).isEqualTo(YoutubeUpdater.class);
    }
    
    @Test
    public void should_return_a_BeInSportUpdater () {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.beinsports.com/replay/category/3361/name/lexpresso");
        /* Then */ assertThat(updaterClass).isEqualTo(BeInSportsUpdater.class);
    }

    @Test
    public void should_return_a_CanalPlusUpdater() {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.canalplus.fr/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(CanalPlusUpdater.class);
    }
    
    @Test
    public void should_return_a_JeuxVideoFrUpdater() {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.jeuxvideo.fr/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(JeuxVideoFRUpdater.class);
    }
    
    @Test
    public void should_return_a_JeuxVideoComUpdater() {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.jeuxvideo.com/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(JeuxVideoComUpdater.class);
    }
    
    @Test
    public void should_return_a_ParleysUpdater() {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.parleys.com/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(ParleysUpdater.class);
    }

    @Test
    public void should_return_a_PluzzUpdater() {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */ Class updaterClass = updaterSelector.of("http://www.pluzz.francetv.fr/show/for/dummies");
        /* Then */ assertThat(updaterClass).isEqualTo(PluzzUpdater.class);
    }

    @Test(expected = RuntimeException.class)
    public void should_reject_empty_url() {
        /* Given */ UpdaterSelector updaterSelector = new UpdaterSelector().setUpdaterCompatibilities(updaterSelectors);
        /* When */  updaterSelector.of("");
    }

}