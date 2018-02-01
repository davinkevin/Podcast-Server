package lan.dk.podcastserver.manager.worker.updater;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lan.dk.utils.IOUtils;
import org.assertj.core.api.Condition;
import org.assertj.core.description.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static io.vavr.API.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 25/12/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class MyCanalUpdaterTest {

    private @Mock SignatureService signatureService;
    private @Mock JsonService jsonService;
    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @InjectMocks MyCanalUpdater updater;

    private Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast
                .builder()
                .id(UUID.randomUUID())
                .url("https://www.mycanal.fr/url/fake")
                .title("A MyCanal Podcast")
                .items(HashSet.<Item>empty().toJavaSet())
                .build();
    }

    @Test
    public void should_sign_podcast() {
        /* Given */
        when(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(IOUtils.fileAsHtml(from("le-tube.html")));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(signatureService.generateMD5Signature(anyString())).then(i -> IOUtils.digest(i.getArgumentAt(0, String.class)));

        /* When */
        String signature = updater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("e9ea94cac29b5114cdc07df006bbf662");
        verify(signatureService, only()).generateMD5Signature(anyString());
    }

    @Test
    public void should_get_items_from_podcast() {
        /* Given */
        when(htmlService.get("https://www.mycanal.fr/url/fake")).thenReturn(IOUtils.fileAsHtml(from("le-tube.html")));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(jsonService.parseUrl(anyString())).then(i -> IOUtils.fileAsJson(withId(i)));
        List("http://media.canal-plus.com/image/76/0/738760.jpg", "http://media.canal-plus.com/image/40/9/732409.jpg",
                "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_733871_H.jpg", "http://media.canal-plus.com/image/76/1/735761.jpg",
                "http://media.canal-plus.com/image/97/4/731974.png", "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_734608_H.jpg",
                "http://media.canal-plus.com/image/31/4/738314.jpg", "http://media.canal-plus.com/image/93/4/737934.jpg",
                "http://media.canal-plus.com/image/39/4/735394.jpg", "http://media.canal-plus.com/image/07/0/735070.jpg",
                "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_736856_H.jpg", "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_736309_H.jpg",
                "http://media.canal-plus.com/image/23/0/734230.jpg", "http://media.canal-plus.com/image/36/8/737368.jpg",
                "http://media.canal-plus.com/wwwplus/image/4/59/2/VIGNETTE_AUTO_733381_H.jpg", "http://media.canal-plus.com/image/89/4/732894.jpg")
                .forEach(i -> when(imageService.getCoverFromURL(i)).thenReturn(Cover.builder().url(i).height(200).width(200).build()));

        /* When */
        Set<Item> items = updater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(16).are(coherent());
    }

    @Test
    public void should_have_a_type() {
        assertThat(updater.type().key()).isEqualTo("MyCanal");
        assertThat(updater.type().name()).isEqualTo("MyCanal");
    }

    @Test
    public void should_be_compatible() {
        assertThat(updater.compatibility("https://www.mycanal.fr/emissions/pid1319-le-tube.html")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(updater.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1);
    }

    public Condition<Item> coherent() {
        return new Condition<Item>() {
            @Override
            public boolean matches(Item value) {
                assertThat(value.getUrl()).isNotEmpty();
                assertThat(value.getCover()).isNotNull();
                assertThat(value.getPubDate()).isNotNull();
                assertThat(value.getTitle()).isNotEmpty();
                assertThat(value.getDescription()).isNotEmpty();
                return true;
            }
        };
    }

    private static String withId(InvocationOnMock i) {
        return Some(i.getArgumentAt(0, String.class))
                .map(v -> v.replace("https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/", ""))
                .map(v -> v.replace("?format=json", ""))
                .map(MyCanalUpdaterTest::from)
                .map(v -> v + ".json")
                .get();
    }

    private static String from(String name) {
        return "/remote/podcast/mycanal/" + name;
    }


}