package lan.dk.podcastserver.manager.worker.dailymotion;

import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;

import static io.vavr.API.None;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionUpdaterTest {

    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SignatureService signatureService;
    private @Mock Validator validator;
    private @Mock JsonService jsonService;
    private @Mock ImageService imageService;
    private @InjectMocks
    DailymotionUpdater dailymotionUpdater;

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
        when(signatureService.fromUrl(eq(String.format(DailymotionUpdater.API_LIST_OF_ITEMS, "karimdebbache")))).thenReturn("aSignature");

        /* When */
        String s = dailymotionUpdater.signatureOf(podcast);

        /* Then */
        assertThat(s).isEqualTo("aSignature");
    }

    @Test
    public void should_get_items() {
        /* Given */
        String karimdebbache = String.format(DailymotionUpdater.API_LIST_OF_ITEMS, "karimdebbache");
        when(jsonService.parseUrl(eq(karimdebbache))).then(i -> IOUtils.fileAsJson("/remote/podcast/dailymotion/user.karimdebbache.json"));

        /* When */
        Set<Item> items = dailymotionUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(10);
    }

    @Test
    public void should_get_empty_list_if_error_during_fetching() {
        /* Given */
        String karimdebbache = String.format(DailymotionUpdater.API_LIST_OF_ITEMS, "karimdebbache");
        when(jsonService.parseUrl(eq(karimdebbache))).thenReturn(None());

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
}
