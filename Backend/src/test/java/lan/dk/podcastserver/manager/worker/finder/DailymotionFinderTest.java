package lan.dk.podcastserver.manager.worker.finder;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 21/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionFinderTest {

    private static final ParseContext PARSER = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build());

    @Mock ImageService imageService;
    @Mock JsonService jsonService;
    @Mock UrlService urlService;
    @InjectMocks DailymotionFinder dailymotionFinder;

    @Before
    public void beforeEach() throws URISyntaxException, IOException {
        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL((String) i.getArguments()[0])));
    }

    @Test
    public void should_find_podcast() throws MalformedURLException {
        /* Given */
        String url = "http://www.dailymotion.com/karimdebbache";
        Cover cover = new Cover("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg", 200, 200);
        when(imageService.getCoverFromURL(eq("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg"))).thenReturn(cover);
        when(jsonService.parse(eq(new URL("https://api.dailymotion.com/user/karimdebbache?fields=avatar_720_url,description,username")))).then(readerFrom("karimdebbache"));

        /* When */
        Podcast podcast = dailymotionFinder.find(url);

        /* Then */
        PodcastAssert
                .assertThat(podcast)
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
        PodcastAssert
                .assertThat(podcast)
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

    private Answer<Optional<Object>> readerFrom(String url) {
        return i -> Optional.of(PARSER.parse(Paths.get(DailymotionFinderTest.class.getResource("/remote/podcast/dailymotion/" + url + ".json").toURI()).toFile()));
    }

}