package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
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

    public static final PodcastServerParameters.Api API = new PodcastServerParameters.Api();
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock JdomService jdomService;
    @Mock HtmlService htmlService;
    @Mock UrlService urlService;
    @InjectMocks YoutubeUpdater youtubeUpdater;

    @Before
    public void beforeEach() {
        API.setYoutube("");
        when(podcastServerParameters.api()).thenReturn(API);
    }

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
    public void should_get_items_for_playlist() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/playlist?list=PLAD454F0807B6CB80")
                .build();

        when(jdomService.parse(anyString())).then(parseFromFile("/remote/podcast/youtube/joueurdugrenier.playlist.xml"));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(15);
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80"));
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

    @Test
    public void should_handle_error_during_signature() throws IOException, JDOMException, URISyntaxException {
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
        doThrow(JDOMException.class).when(jdomService).parse(anyString());


        /* When */
        String signature = youtubeUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEmpty();
    }

    @Test
    public void should_return_empty_if_parsing_error() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/feeds/videos.xml?playlist_id=PLYMLK0zkSFQTblsW2biu2m4suKvoomN5D")
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
        doThrow(JDOMException.class).when(jdomService).parse(anyString());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    @Test
    public void should_return_empty_set_because_html_page_not_found() throws IOException, JDOMException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        doThrow(IOException.class).when(htmlService).connectWithDefault(any(String.class));
        doThrow(IOException.class).when(jdomService).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id="));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    @Test
    public void should_return_empty_set_because_of_data_tag_not_find() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        Connection connection = mock(Connection.class);
        Document document = mock(Document.class);
        Elements elements = mock(Elements.class);
        when(htmlService.connectWithDefault(any(String.class))).thenReturn(connection);
        when(connection.get()).thenReturn(document);
        when(document.select(anyString())).thenReturn(elements);
        doThrow(IOException.class).when(jdomService).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id="));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id="));
        verify(htmlService, only()).connectWithDefault(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    @Test
    public void should_get_items_with_API_from_channel() throws IOException {
        /* Given */
        API.setYoutube("FOO");
        Podcast podcast = Podcast.builder().url("http://www.youtube.com/user/joueurdugrenier").build();

        Connection connection = mock(Connection.class);
        Document document = mock(Document.class);
        Elements elements = mock(Elements.class);
        Element theFirstElement = mock(Element.class);
        when(htmlService.connectWithDefault(any(String.class))).thenReturn(connection);
        when(connection.get()).thenReturn(document);
        when(document.select(anyString())).thenReturn(elements);
        when(elements.first()).thenReturn(theFirstElement);
        when(theFirstElement.attr(anyString())).thenReturn("UCVHFbqXqoYvEWM1Ddxl0QDg");
        when(urlService.getReaderFromURL(anyString())).then(readerFrom("/remote/podcast/youtube/joueurdugrenier.json"));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(50);
    }
    
    @Test
    public void should_failed_during_fetch_API() throws JDOMException, IOException {
        /* Given */
        API.setYoutube("FOO");
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
        doThrow(IOException.class).when(urlService).getReaderFromURL(anyString());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    private Answer<Object> readerFrom(String url) {
        return i -> Files.newBufferedReader(Paths.get(YoutubeUpdater.class.getResource(url).toURI()));
    }

    private Answer<Object> parseFromFile(String file) throws JDOMException, IOException, URISyntaxException {
        return invocationOnMock -> new SAXBuilder().build(Paths.get(RSSUpdaterTest.class.getResource(file).toURI()).toFile());
    }

}