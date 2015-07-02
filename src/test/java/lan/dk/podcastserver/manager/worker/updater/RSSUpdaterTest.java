package lan.dk.podcastserver.manager.worker.updater;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RSSUpdaterTest {

    public static final String PODCAST_URL = "/remote/podcast/rss.appload.xml";
    public static final String MOCK_URL = "http://mockUrl.com/";
    public static Podcast RSS_APPLOAD;

    @Captor ArgumentCaptor<String> urlArgumentCaptor;

    @Mock SignatureService signatureService;
    @Mock JdomService jdomService;
    @Mock ImageService imageService;
    @InjectMocks RSSUpdater rssUpdater;

    @Before
    public void beforeEach() throws JDOMException, IOException {
        RSS_APPLOAD = new Podcast().setUrl(PODCAST_URL);

        when(jdomService.jdom2Parse(eq(PODCAST_URL)))
                .then(invocationOnMock -> new SAXBuilder().build(Paths.get(RSSUpdaterTest.class.getResource(PODCAST_URL).toURI()).toFile()));

        when(jdomService.jdom2Parse(not(eq(PODCAST_URL))))
                .thenThrow(new JDOMException());

    }

    @Test
    public void should_get_items() throws JDOMException, IOException {
        /* When */ Set<Item> items = rssUpdater.getItems(RSS_APPLOAD);
        /* Then */
        verify(jdomService, times(1)).jdom2Parse(urlArgumentCaptor.capture());
        assertThat(urlArgumentCaptor.getValue()).isEqualTo(PODCAST_URL);
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
        /* Given */ Podcast podcast = new Podcast().setUrl(MOCK_URL);
        /* When */  Set<Item> items = rssUpdater.getItems(podcast);
        /* Then */  assertThat(items).isEmpty();
    }

    @Test
    public void should_call_signature_from_url() {
        /* Given */
        /* When */  rssUpdater.signatureOf(RSS_APPLOAD);
        /* Then */
        verify(signatureService, times(1)).generateSignatureFromURL(urlArgumentCaptor.capture());
        assertThat(urlArgumentCaptor.getValue()).isEqualTo(PODCAST_URL);
    }

    @Test
    public void should_return_his_type() {
        AbstractUpdater.Type type = rssUpdater.type();
        assertThat(type.key()).isEqualTo("RSS");
        assertThat(type.name()).isEqualTo("RSS");
    }


}