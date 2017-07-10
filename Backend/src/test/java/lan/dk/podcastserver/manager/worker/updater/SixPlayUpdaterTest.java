package lan.dk.podcastserver.manager.worker.updater;

import javaslang.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 22/12/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class SixPlayUpdaterTest {

    private @Mock SignatureService signatureService;
    private @Mock HtmlService htmlService;
    private @Mock JsonService jsonService;
    private @Mock ImageService imageService;
    private @InjectMocks SixPlayUpdater updater;

    private final Podcast turbo = Podcast.builder()
                .title("Turbo")
                .url("http://www.6play.fr/turbo-p_884")
            .build();

    @Test
    public void should_extract_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/turbo-p_884.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(imageService.getCoverFromURL(anyString())).thenReturn(Cover.DEFAULT_COVER);
        /* When */
        Set<Item> items = updater.getItems(turbo);

        /* Then */
        assertThat(items).hasSize(186).are(allValid());
    }

    @Test
    public void should_do_signature() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/turbo-p_884.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(signatureService.generateMD5Signature(anyString())).thenCallRealMethod();
        /* WHEN  */
        String signature = updater.signatureOf(turbo);
        /* THEN  */
        assertThat(signature).isNotEmpty();
    }

    @Test
    public void should_have_the_same_signature_twice() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/turbo-p_884.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(signatureService.generateMD5Signature(anyString())).thenCallRealMethod();
        /* WHEN  */
        String s1 = updater.signatureOf(turbo);
        String s2 = updater.signatureOf(turbo);
        /* THEN  */
        assertThat(s1).isEqualToIgnoringCase(s2);
    }

    @Test
    public void should_have_type() {
        assertThat(updater.type().key()).isEqualTo("SixPlay");
        assertThat(updater.type().name()).isEqualTo("6Play");
    }

    @Test
    public void should_be_only_compatible_with_6play_url() {
        assertThat(updater.compatibility(null)).isGreaterThan(1);
        assertThat(updater.compatibility("foo")).isGreaterThan(1);
        assertThat(updater.compatibility("http://www.6play.fr/test")).isEqualTo(1);
    }

    private Condition<Item> allValid() {
       Predicate<Item> p = ((Predicate<Item>) v -> StringUtils.isNotEmpty(v.getUrl()))
                .and(v -> StringUtils.isNotEmpty(v.getTitle()))
                .and(v -> nonNull(v.getPubDate()))
                .and(v -> StringUtils.isNotEmpty(v.getUrl()))
                .and(v -> nonNull(v.getLength()))
                .and(v -> nonNull(v.getCover()));

        return new Condition<>(p, "Should have coherent fields");
    }
}