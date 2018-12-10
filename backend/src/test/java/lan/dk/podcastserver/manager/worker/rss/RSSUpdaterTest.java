package lan.dk.podcastserver.manager.worker.rss;

import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import com.github.davinkevin.podcastserver.service.JdomService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.vavr.API.None;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RSSUpdaterTest {

    private static final String PODCAST_APPLOAD_URL = "/remote/podcast/rss/rss.appload.xml";
    private static final String MOCK_URL = "http://mockUrl.com/";
    private Podcast rssAppload;

    private @Mock SignatureService signatureService;
    private @Mock JdomService jdomService;
    private @Mock ImageService imageService;
    private @InjectMocks
    RSSUpdater rssUpdater;

    @Before
    public void beforeEach() throws JDOMException, IOException, URISyntaxException {
        rssAppload = Podcast.builder().url(MOCK_URL).build();

        when(jdomService.parse(eq(MOCK_URL))).thenReturn(IOUtils.fileAsXml(PODCAST_APPLOAD_URL));
        when(jdomService.parse(not(eq(MOCK_URL)))).thenReturn(None());
    }

    @Test
    public void should_get_items() throws JDOMException, IOException {
        /* Given */
        when(imageService.getCoverFromURL(anyString())).then(this::createCover);

        /* When */
        Set<Item> items = rssUpdater.getItems(rssAppload);

        /* Then */
        verify(jdomService, times(1)).parse(eq(MOCK_URL));
        verify(imageService, times(2)).getCoverFromURL(anyString());
        assertThat(items).hasSize(217)
                .anySatisfy(i -> assertThat(i.getCover()).isNotEqualTo(Cover.DEFAULT_COVER));
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
        /* Then */ verify(signatureService, times(1)).fromUrl(eq(MOCK_URL));
    }

    @Test
    public void should_return_his_type() {
        Type type = rssUpdater.type();
        assertThat(type.key()).isEqualTo("RSS");
        assertThat(type.name()).isEqualTo("RSS");
    }

    private Cover createCover(InvocationOnMock i) {
        return Cover.builder().url(i.getArgument(0)).build();
    }
}
