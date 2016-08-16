package lan.dk.podcastserver.manager.worker.updater;

import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 06/12/2015 for Podcast Server
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ParleysUpdaterTest {

    @Mock ImageService imageService;
    @Mock JsonService jsonService;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @InjectMocks ParleysUpdater parleysUpdater;

    Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast.builder()
                .title("Devoxx FR 2015")
                .url("https://www.parleys.com/channel/devoxx-france-2015")
                .build();
    }

    @Test
    public void should_sign_the_podcast() throws IOException {
        when(jsonService.parseUrl(
                eq("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=1&text=&orderBy=date"))
        ).then(i -> IOUtils.fileAsJson("/remote/podcast/parleys/devoxx-france-2015.json"));
        when(signatureService.generateMD5Signature(anyString())).thenReturn("aSignatureWithString");

        /* When */
        String signature = parleysUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignatureWithString");
    }

    @Test
    public void should_fail_on_signature() throws IOException {
        /* Given */
        when(jsonService.parseUrl(anyString())).thenReturn(Option.none());

        /* When */
        String signature = parleysUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEmpty();
    }

    @Test
    public void should_get_items() throws IOException {
        /* Given */
        when(jsonService.parseUrl(or(
                eq("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=4&text=&orderBy=date"),
                eq("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=1&text=&orderBy=date"))
        )).then(i -> IOUtils.fileAsJson("/remote/podcast/parleys/devoxx-france-2015.json"));
        when(jsonService.parseUrl(not(or(
                eq("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=4&text=&orderBy=date"),
                eq("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=1&text=&orderBy=date"))))
        ).then(i -> IOUtils.fileAsJson(urlToUri(i)));
        /*when(jsonService.from(eq(new URL("http://api.parleys.com/api/presentation.json/5534a8e6e4b056a8233822ab?view=true")))).thenReturn(Optional.empty());*/

        /* When */
        Set<Item> items = parleysUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(3);
    }

    @Test
    public void should_failed_on_get_items() throws IOException {
        /* Given */
        podcast.setUrl("http://another.url.without/pattern");
        when(jsonService.parseUrl(anyString())).thenReturn(Option.none());

        /* When */
        Set<Item> items = parleysUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_return_type() {
        assertThat(parleysUpdater.type().key()).isEqualTo("Parleys");
        assertThat(parleysUpdater.type().name()).isEqualTo("Parleys");
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "https://foo.bar.com/";
        /* When */
        Integer compatibility = parleysUpdater.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        String url = "https://parleys.com/foo/bar";
        /* When */
        Integer compatibility = parleysUpdater.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(1);
    }

    private String urlToUri(InvocationOnMock i) {
        return String.format("/remote/podcast/parleys/%s.json", getIdFromUrl(i.getArgumentAt(0, String.class)));
    }

    private String getIdFromUrl(Object o) {
        return o.toString().replace("http://api.parleys.com/api/presentation.json/", "").replace("?view=true", "");
    }

}