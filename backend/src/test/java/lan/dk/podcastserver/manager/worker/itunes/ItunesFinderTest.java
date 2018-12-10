package lan.dk.podcastserver.manager.worker.itunes;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.rss.RSSFinder;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 12/05/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ItunesFinderTest {

    @Mock private RSSFinder rssFinder;
    @Mock private JsonService jsonService;
    @InjectMocks private ItunesFinder finder;

    @Test
    public void should_be_compatible_with_itunes_url() {
        /* GIVEN */
        String url = "https://itunes.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2";
        /* WHEN  */
        Integer compatibilityLevel = finder.compatibility(url);
        /* THEN  */
        assertThat(compatibilityLevel).isEqualTo(1);
    }


    @Test
    public void should_not_be_compatible() {
        /* GIVEN */
        String url = "https://foo.bar.com/fr/podcast/foo/idbar";
        /* WHEN  */
        Integer compatibilityLevel = finder.compatibility(url);
        /* THEN  */
        assertThat(compatibilityLevel).isGreaterThan(1);
    }

    @Test
    public void should_find_url() {
        /* GIVEN */
        String url = "https://itunes.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2";
        Podcast p = Podcast.builder().build();
        when(jsonService.parseUrl("https://itunes.apple.com/lookup?id=1278255446"))
                .thenReturn(IOUtils.fileAsJson(from("lookup.json")));
        when(rssFinder.find("https://www.virginradio.fr/cauet-s-lache/podcasts.podcast"))
                .thenReturn(p);
        /* WHEN  */
        Podcast podcast = finder.find(url);
        /* THEN  */
        assertThat(podcast).isSameAs(p);
    }

    @Test
    public void should_return_default_podcast_if_nothing_found() {
        /* GIVEN */
        String url = "https://foo.bar.com/ofejaoieaf/aekofjaeoi";
        /* WHEN  */
        Podcast podcast = finder.find(url);
        /* THEN  */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST);
    }

    private String from(String name) {
        return String.format("/remote/podcast/itunes/%s", name);
    }
}