package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 12/10/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class GulliDownloaderTest {

    private @Mock HtmlService htmlService;
    private @Mock JsonService jsonService;
    /*
    private @Mock UrlService urlService;
    private @Mock WGetFactory wGetFactory;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastRepository podcastRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    */
    private @InjectMocks GulliDownloader gulliDownloader;

    @Before
    public void beforeEach() {
        gulliDownloader.setItem(Item.builder()
                    .title("Gulli Item")
                    .url("http://replay.gulli.fr/")
                .build()
        );

    }

    @Test
    public void should_be_an_http_downloader() {
        assertThat(gulliDownloader).isInstanceOfAny(HTTPDownloader.class);
    }

    @Test
    public void should_get_item_url_for_different_item() {
        /* Given */
        Item anotherItem = Item.builder().url("http://foo.bar.com/boo").build();

        /* When */
        String url = gulliDownloader.getItemUrl(anotherItem);

        /* Then */
        assertThat(url).isEqualTo(anotherItem.getUrl());
    }

    @Test
    public void should_find_url_for_gulli_item() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/embed.VOD68526621555000.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));

        /* When */
        String url = gulliDownloader.getItemUrl(gulliDownloader.getItem());
        String secondUrl = gulliDownloader.getItemUrl(gulliDownloader.getItem());

        /* Then */
        assertThat(url).isEqualTo("http://gulli-replay-mp4.scdn.arkena.com/68526621555000/68526621555000_1500.mp4");
        assertThat(secondUrl).isEqualTo("http://gulli-replay-mp4.scdn.arkena.com/68526621555000/68526621555000_1500.mp4");
        verify(htmlService, times(1)).get(anyString());
        verify(jsonService, times(1)).parse(anyString());
    }

    @Test
    public void should_not_find_element_if_playlist_item_not_set() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/embed.VOD68526621555000.without.position.html"));

        /* When */
        /* Then */
        assertThatThrownBy(() -> gulliDownloader.getItemUrl(gulliDownloader.getItem()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void should_be_compatible() {
        assertThat(gulliDownloader.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(gulliDownloader.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isGreaterThan(1);
    }



}
