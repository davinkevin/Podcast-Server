package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.SignatureService;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 30/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class YoutubeUpdaterTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock JdomService jdomService;
    @Mock HtmlService htmlService;
    @InjectMocks YoutubeUpdater youtubeUpdater;

    @Test
    public void should_be_of_type_youtube() {
        /* Given */
        AbstractUpdater.Type type = youtubeUpdater.type();

        /* Then */
        assertThat(type).isNotNull();
        assertThat(type.key()).isEqualTo("Youtube");
        assertThat(type.name()).isEqualTo("Youtube");
    }

    @Test
    public void should_get_items_for_channel() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        Connection connection = mock(Connection.class);
        Document document = mock(Document.class);
        Elements elements = mock(Elements.class);
        Element theFirstElement = mock(Element.class);
        when(htmlService.connectWithDefault(any(String.class))).thenReturn(connection);
        when(connection.get()).thenReturn(document);
        when(document.select(anyString())).thenReturn(elements);
        when(elements.first()).thenReturn(theFirstElement);
        when(theFirstElement.attr(anyString())).thenReturn("UCVHFbqXqoYvEWM1Ddxl0QDg");
        when(jdomService.parse(anyString())).then(parseFromFile("/remote/podcast/youtube.androiddevelopers.xml"));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(15);
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg"));
        verify(htmlService, only()).connectWithDefault(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    @Test
    public void should_generate_signature() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        Connection connection = mock(Connection.class);
        Document document = mock(Document.class);
        Elements elements = mock(Elements.class);
        Element theFirstElement = mock(Element.class);
        when(htmlService.connectWithDefault(any(String.class))).thenReturn(connection);
        when(connection.get()).thenReturn(document);
        when(document.select(anyString())).thenReturn(elements);
        when(elements.first()).thenReturn(theFirstElement);
        when(theFirstElement.attr(anyString())).thenReturn("UCVHFbqXqoYvEWM1Ddxl0QDg");
        when(jdomService.parse(anyString())).then(parseFromFile("/remote/podcast/youtube.androiddevelopers.xml"));
        when(signatureService.generateMD5Signature(anyString())).thenReturn("Signature");

        /* When */
        String signature = youtubeUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("Signature");
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg"));
        verify(htmlService, only()).connectWithDefault(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    private Answer<Object> parseFromFile(String file) throws JDOMException, IOException, URISyntaxException {
        return invocationOnMock -> new SAXBuilder().build(Paths.get(RSSUpdaterTest.class.getResource(file).toURI()).toFile());
    }

}