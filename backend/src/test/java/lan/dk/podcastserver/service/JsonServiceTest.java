package lan.dk.podcastserver.service;

import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davinkevin.podcastserver.IOUtils;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
public class JsonServiceTest {

    private UrlService urlService;
    private JsonService jsonService;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void beforeEach() {
        urlService = mock(UrlService.class);
        jsonService = new JsonService(urlService, mapper);
    }

    @Test
    public void should_read_json_from_files() {
        /* Given */
        when(urlService.asReader(any())).thenReturn(IOUtils.fileAsReader("/remote/podcast/dailymotion/user.karimdebbache.json"));

        /* When */
        Option<DocumentContext> aFakeUrl = jsonService.parseUrl("http://foo.com/");

        /* Then */
        assertThat(aFakeUrl.isDefined()).isTrue();
    }

    @Test
    public void should_return_empty_if_error_during_parsing() {
        /* Given */
        doThrow(UncheckedIOException.class).when(urlService).asReader(any());

        /* When */
        Option<DocumentContext> aFakeUrl = jsonService.parseUrl("http://foo.com/");

        /* Then */
        assertThat(aFakeUrl.isEmpty()).isTrue();
    }
    
    @Test
    public void should_parse_from_string() {
        /* Given */
        String object = "{ \"foo\" : \"bar\"}";
        /* When */
        DocumentContext parse = jsonService.parse(object);

        /* Then */
        assertThat(parse.read("foo", String.class)).isEqualTo("bar");
    }

    @Test
    public void should_parse_from_string_into_array() {
        /* Given */
        String object = "{ \"foo\" : [{\"bar\":\"bar\"}]}";
        /* When */
        DocumentContext parse = jsonService.parse(object);

        /* Then */
        assertThat(parse.read("foo[0].bar", String.class)).isEqualTo("bar");
    }

    @Test
    public void should_return_empty_if_error_during_parsing_string() {
        /* Given */
        String object = "}{{{";

        /* When */
        assertThatThrownBy(() -> jsonService.parse(object))
                /* Then */
                .isInstanceOf(InvalidJsonException.class);

    }
}
