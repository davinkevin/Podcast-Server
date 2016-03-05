package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 06/12/2015 for Podcast Server
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ParleysUpdaterTest {

    public static final JSONParser PARSER = new JSONParser();
    @Mock ImageService imageService;
    @Mock UrlService urlService;
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
                .url("https://www.parleys.com/channel/devoxx-france-2015/")
                .build();
        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL((String) i.getArguments()[0])));
    }

    @Test
    public void should_sign_the_podcast() throws IOException {
        when(jsonService.from(eq(new URL("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=1&text=&orderBy=date")))).then(readerFrom("devoxx-france-2015.json"));
        when(signatureService.generateMD5Signature(anyString())).thenReturn("aSignatureWithString");

        /* When */
        String signature = parleysUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignatureWithString");
    }

    @Test
    public void should_fail_on_signature() throws IOException {
        /* Given */
        when(jsonService.from(any(URL.class))).thenReturn(Optional.empty());

        /* When */
        String signature = parleysUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEmpty();
    }

    @Test
    public void should_get_items() throws IOException {
        /* Given */
        when(jsonService.from(or(
                eq(new URL("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=4&text=&orderBy=date")),
                eq(new URL("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=1&text=&orderBy=date")))
        )).then(readerFrom("devoxx-france-2015.json"));
        when(jsonService.from(not(or(
                eq(new URL("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=4&text=&orderBy=date")),
                eq(new URL("http://api.parleys.com/api/presentations.json/devoxx-france-2015?index=0&size=1&text=&orderBy=date")))))
        ).then(itemFromId());
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
        when(jsonService.from(any(URL.class))).thenReturn(Optional.empty());

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
    
    private Answer<Optional<Object>> itemFromId() {
        return i -> Optional.of(PARSER.parse(Files.newBufferedReader(Paths.get(ParleysUpdaterTest.class.getResource(String.format("/remote/podcast/parleys/%s.json", i.getArguments()[0].toString().replace("http://api.parleys.com/api/presentation.json/", "").replace("?view=true", ""))).toURI()))));
    }

    private Answer<Optional<Object>> readerFrom(String url) {
        return i -> Optional.of(PARSER.parse(Files.newBufferedReader(Paths.get(ParleysUpdaterTest.class.getResource("/remote/podcast/parleys/" + url).toURI()))));
    }

}