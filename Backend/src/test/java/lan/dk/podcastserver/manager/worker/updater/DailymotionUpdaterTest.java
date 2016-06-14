package lan.dk.podcastserver.manager.worker.updater;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.validation.Validator;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionUpdaterTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock JsonService jsonService;
    @Mock ImageService imageService;
    @Spy UrlService urlService;
    @InjectMocks DailymotionUpdater dailymotionUpdater;

    private static final ParseContext PARSER = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build());
    Podcast podcast;


    @Before
    public void beforeEach() {
        podcast = Podcast.builder()
                .title("Karim Debbache")
                .url("http://www.dailymotion.com/karimdebbache")
                .build();

    }

    @Test
    public void should_sign_from_url() {
        /* Given */
        when(signatureService.generateSignatureFromURL(eq(String.format(DailymotionUpdater.API_LIST_OF_ITEMS, "karimdebbache")))).thenReturn("aSignature");

        /* When */
        String s = dailymotionUpdater.signatureOf(podcast);

        /* Then */
        assertThat(s).isEqualTo("aSignature");
    }

    @Test
    public void should_get_items() throws MalformedURLException {
        /* Given */
        URL karimdebbache = new URL(String.format(DailymotionUpdater.API_LIST_OF_ITEMS, "karimdebbache"));
        when(jsonService.parse(eq(karimdebbache))).then(asJson("user.karimdebbache.json"));

        /* When */
        Set<Item> items = dailymotionUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(10);
    }

    @Test
    public void should_get_empty_list_if_error_during_fetching() throws MalformedURLException {
        /* Given */
        URL karimdebbache = new URL(String.format(DailymotionUpdater.API_LIST_OF_ITEMS, "karimdebbache"));
        when(jsonService.parse(eq(karimdebbache))).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = dailymotionUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test(expected = RuntimeException.class)
    public void should_get_empty_list_if_error_of_parsing_url() {
        /* Given */
        podcast.setUrl("http://foo.bar/goo");

        /* When */
        dailymotionUpdater.signatureOf(podcast);
    }

    @Test
    public void should_have_type() {
        assertThat(dailymotionUpdater.type().name()).isEqualTo("Dailymotion");
        assertThat(dailymotionUpdater.type().key()).isEqualTo("Dailymotion");
    }

    private Answer<Optional<DocumentContext>> asJson(String file) {
        return i -> Optional.of(PARSER.parse(Paths.get(DailymotionUpdaterTest.class.getResource("/remote/downloader/dailymotion/" + file).toURI()).toFile()));
    }

}