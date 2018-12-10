package lan.dk.podcastserver.manager.worker.dailymotion;

import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.MalformedURLException;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 21/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionFinderTest {

    private @Mock ImageService imageService;
    private @Mock JsonService jsonService;
    private @InjectMocks
    DailymotionFinder dailymotionFinder;

    @Test
    public void should_find_podcast() throws MalformedURLException {
        /* Given */
        String url = "http://www.dailymotion.com/karimdebbache";
        Cover cover = Cover.builder().url("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg").width(200).height(200).build();
        when(imageService.getCoverFromURL(eq("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg"))).thenReturn(cover);
        when(jsonService.parseUrl(eq("https://api.dailymotion.com/user/karimdebbache?fields=avatar_720_url,description,username")))
                .then(i -> IOUtils.fileAsJson("/remote/podcast/dailymotion/karimdebbache.json"));

        /* When */
        Podcast podcast = dailymotionFinder.find(url);

        /* Then */
        assertThat(podcast)
                .hasDescription("CHROMA est une CHROnique de cinéMA sur Dailymotion, dont la première saison se compose de dix épisodes, à raison d’un par mois, d’une durée comprise entre quinze et vingt minutes. Chaque épisode est consacré à un film en particulier.")
                .hasCover(cover)
                .hasTitle("karimdebbache")
                .hasType("Dailymotion");
    }
    
    @Test
    public void should_not_find_podcast() {
        /* Given */
        String url = "http://iojafea/fake/url";
        /* When */
        Podcast podcast = dailymotionFinder.find(url);
        /* Then */
        assertThat(podcast)
                .hasUrl(url)
                .hasType("Dailymotion")
                .hasCover(new Cover());
    }

    @Test
    public void should_be_compatible() {
        assertThat(dailymotionFinder.compatibility("http://www.dailymotion.com/karimdebbache")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(dailymotionFinder.compatibility("http://iojafea/fake/url")).isGreaterThan(1);
    }
}
