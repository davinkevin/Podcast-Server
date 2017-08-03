package lan.dk.podcastserver.manager.worker.updater;

import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.SignatureService;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RSSUpdaterTest {

    private static final String PODCAST_APPLOAD_URL = "/remote/podcast/rss.appload.xml";
    private static final String MOCK_URL = "http://mockUrl.com/";
    private Podcast rssAppload;

    @Mock SignatureService signatureService;
    @Mock JdomService jdomService;
    @Mock ImageService imageService;
    @InjectMocks RSSUpdater rssUpdater;

    @Before
    public void beforeEach() throws JDOMException, IOException, URISyntaxException {
        rssAppload = Podcast.builder().url(MOCK_URL).build();
        Path xmlFile = Paths.get(RSSUpdaterTest.class.getResource(PODCAST_APPLOAD_URL).toURI());

        when(jdomService.parse(eq(MOCK_URL))).thenReturn(Option.of(new SAXBuilder().build(xmlFile.toFile())));
        when(jdomService.parse(not(eq(MOCK_URL)))).thenReturn(Option.none());
    }

    @Test
    public void should_get_items() throws JDOMException, IOException {
        /* When */ Set<Item> items = rssUpdater.getItems(rssAppload);
        /* Then */
        verify(jdomService, times(1)).parse(eq(MOCK_URL));
        assertThat(items).hasSize(217);
    }

    @Test
    public void should_return_null_if_not_updatable_podcast() {
        /* Given */ Podcast podcastNotUpdatable = new Podcast().setUrl("http://notUpdatable.com");
        /* When */  Set<Item> items = rssUpdater.getItems(podcastNotUpdatable);
        /* Then */  assertThat(items).isEmpty();
    }

    @Test
    public void should_return_an_empty_set() {
        /* Given */ Podcast podcast = Podcast.builder().url("http://foo.bar.fake.url/").build();
        /* When */  Set<Item> items = rssUpdater.getItems(podcast);
        /* Then */  assertThat(items).isEmpty();
    }

    @Test
    public void should_call_signature_from_url() {
        /* When */ rssUpdater.signatureOf(rssAppload);
        /* Then */ verify(signatureService, times(1)).generateSignatureFromURL(eq(MOCK_URL));
    }

    @Test
    public void should_return_his_type() {
        AbstractUpdater.Type type = rssUpdater.type();
        assertThat(type.key()).isEqualTo("RSS");
        assertThat(type.name()).isEqualTo("RSS");
    }
}
