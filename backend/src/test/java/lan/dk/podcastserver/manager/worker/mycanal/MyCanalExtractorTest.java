package lan.dk.podcastserver.manager.worker.mycanal;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.vavr.API.Some;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 25/12/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class MyCanalExtractorTest {

    private @Mock HtmlService htmlService;
    private @Mock JsonService jsonService;
    private @InjectMocks
    MyCanalExtractor extractor;

    private Item item;

    @Before
    public void beforeEach() {
        item = Item.builder()
                .url("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")
                .build();
    }

    @Test
    public void should_extract() {
        /* GIVEN */
        when(htmlService.get("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")).thenReturn(IOUtils.fileAsHtml(from("1474195.html")));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgument(0)));
        when(jsonService.parseUrl(anyString())).then(i -> IOUtils.fileAsJson(withId(i)));
        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(item);
        /* THEN  */
        assertThat(downloadingItem.getItem()).isSameAs(item);
        assertThat(downloadingItem.url()).containsOnly("https://strcpluscplus-vh.akamaihd.net/i/1712/16/1191740_16_,200k,400k,800k,1500k,.mp4.csmil/master.m3u8");
        assertThat(downloadingItem.getFilename()).isEqualTo("1474195.mp4");
    }

    private static String withId(InvocationOnMock i) {
        return Some(i.getArgument(0))
                .map(String.class::cast)
                .map(v -> v.replace("https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/", ""))
                .map(v -> v.replace("?format=json", ""))
                .map(MyCanalExtractorTest::from)
                .map(v -> v + ".json")
                .get();
    }


    private static String from(String name) {
        return "/remote/podcast/mycanal/" + name;
    }

    @Test
    public void should_be_compatible() {
        assertThat(extractor.compatibility("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(extractor.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1);
    }

}