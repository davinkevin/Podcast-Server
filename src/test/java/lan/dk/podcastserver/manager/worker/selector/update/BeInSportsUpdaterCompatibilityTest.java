package lan.dk.podcastserver.manager.worker.selector.update;
import lan.dk.podcastserver.manager.worker.updater.BeInSportsUpdater;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class BeInSportsUpdaterCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ BeInSportUpdaterCompatibility beInSportUpdaterCompatibility = new BeInSportUpdaterCompatibility();
        /* When */ Integer compatibility = beInSportUpdaterCompatibility.compatibility("http://www.beinsports.fr/replay/category/3361/name/lexpresso");
        /* Then */ assertThat(compatibility).isLessThan(5);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ BeInSportUpdaterCompatibility youtubeUpdateSelector = new BeInSportUpdaterCompatibility();
        /* When */ Integer compatibility = youtubeUpdateSelector.compatibility("http://www.wrong.url/user");
        /* Then */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_BeInSportUpdate_class () {
        /* Given */ BeInSportUpdaterCompatibility youtubeUpdateSelector = new BeInSportUpdaterCompatibility();
        /* When */ Class clazz = youtubeUpdateSelector.updater();
        /* Then */ assertThat(clazz).isEqualTo(BeInSportsUpdater.class);
    }

}