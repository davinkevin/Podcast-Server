package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.UrlService;
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
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RSSUpdaterTest {

    public static final String PODCAST_APPLOAD_URL = "/remote/podcast/rss.appload.xml";
    public static final String MOCK_URL = "http://mockUrl.com/";
    public Podcast rssAppload;

    @Mock UrlService urlService;
    @Mock SignatureService signatureService;
    @Mock JdomService jdomService;
    @Mock ImageService imageService;
    @InjectMocks RSSUpdater rssUpdater;

    @Before
    public void beforeEach() throws JDOMException, IOException, URISyntaxException {
        rssAppload = new Podcast().setUrl(MOCK_URL);
        URL url = new URL(MOCK_URL);
        Path xmlFile = Paths.get(RSSUpdaterTest.class.getResource(PODCAST_APPLOAD_URL).toURI());

        when(jdomService.parse(eq(url))).thenReturn(Optional.of(new SAXBuilder().build(xmlFile.toFile())));
        when(jdomService.parse(not(eq(url)))).thenReturn(Optional.empty());
        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL(((String) i.getArguments()[0]))));
    }

    @Test
    public void should_get_items() throws JDOMException, IOException {
        /* When */ Set<Item> items = rssUpdater.getItems(rssAppload);
        /* Then */
        verify(jdomService, times(1)).parse(eq(new URL(MOCK_URL)));
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
        /* Given */ Podcast podcast = new Podcast().setUrl("http://foo.bar.fake.url/");
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