package lan.dk.podcastserver.manager.worker.gulli;

import com.github.davinkevin.podcastserver.service.HtmlService;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
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
 * Created by kevin on 11/12/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class GulliExtractorTest {

    private @Mock HtmlService htmlService;
    private @Mock JsonService jsonService;
    private @InjectMocks
    GulliExtractor extractor;
    private Item item;

    @Before
    public void beforeEach() {
        item = Item.builder()
                .title("Gulli Item")
                .url("http://replay.gulli.fr/")
                .build();
    }

    @Test
    public void should_find_url_for_gulli_item() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/embed.VOD68526621555000.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgument(0)));

        /* When */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* Then */
        assertThat(downloadingItem.url()).containsOnly("http://gulli-replay-mp4.scdn.arkena.com/68526621555000/68526621555000_1500.mp4");
        verify(htmlService, times(1)).get(anyString());
        verify(jsonService, times(1)).parse(anyString());
    }

    @Test
    public void should_not_find_element_if_playlist_item_not_set() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/embed.VOD68526621555000.without.position.html"));

        /* When */
        assertThatThrownBy(() -> extractor.extract(item))

        /* Then */
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void should_be_compatible() {
        assertThat(extractor.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(extractor.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isGreaterThan(1);
    }

}