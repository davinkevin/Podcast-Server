package lan.dk.podcastserver.manager.worker.youtube;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import com.github.davinkevin.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.properties.Api;
import com.github.davinkevin.podcastserver.IOUtils;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.vavr.API.List;
import static io.vavr.API.None;
import static com.github.davinkevin.podcastserver.IOUtils.fileAsXml;
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

    private @Mock Api api;
    // private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SignatureService signatureService;
    // private @Mock Validator validator;
    private @Mock JdomService jdomService;
    private @Mock JsonService jsonService;
    private @Mock HtmlService htmlService;
    private @InjectMocks
    YoutubeUpdater youtubeUpdater;

    @Before
    public void beforeEach() {
        when(api.getYoutube()).thenReturn("");
    }

    @Test
    public void should_be_of_type_youtube() {
        /* Given */
        Type type = youtubeUpdater.type();

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
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"));
        when(jdomService.parse(anyString())).then(i -> fileAsXml("/remote/podcast/youtube/youtube.androiddevelopers.xml"));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(15);
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg"));
        verify(htmlService, only()).get(eq("https://www.youtube.com/user/androiddevelopers"));
    }


    @Test
    public void should_get_items_for_playlist() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/playlist?list=PLAD454F0807B6CB80")
                .build();

        when(jdomService.parse(anyString())).thenReturn(fileAsXml("/remote/podcast/youtube/joueurdugrenier.playlist.xml"));

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

        when(htmlService.get(any(String.class))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"));
        when(jdomService.parse(anyString())).thenReturn(fileAsXml("/remote/podcast/youtube/youtube.androiddevelopers.xml"));
        when(signatureService.fromText(anyString())).thenReturn("Signature");

        /* When */
        String signature = youtubeUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("Signature");
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg"));
        verify(htmlService, only()).get(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    @Test
    public void should_handle_error_during_signature() throws IOException, JDOMException, URISyntaxException {
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        when(htmlService.get(any(String.class))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"));
        when(jdomService.parse(anyString())).thenReturn(None());


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

        when(htmlService.get(anyString())).thenReturn(None());
        when(jdomService.parse(anyString())).thenReturn(None());

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

        when(htmlService.get(any(String.class))).thenReturn(None());
        when(jdomService.parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id="))).thenReturn(None());

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

        when(htmlService.get(any(String.class))).thenReturn(None());
        when(jdomService.parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id="))).thenReturn(None());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
        verify(jdomService, only()).parse(eq("https://www.youtube.com/feeds/videos.xml?channel_id="));
        verify(htmlService, only()).get(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    @Test
    public void should_get_items_with_API_from_channel() throws IOException, URISyntaxException {
        /* Given */
        String page1 = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO";
        String page2 = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO&pageToken=CDIQAA";

        when(api.getYoutube()).thenReturn("FOO");
        Podcast podcast = Podcast.builder().url("https://www.youtube.com/user/joueurdugrenier").items(HashSet.<Item>empty().toJavaSet()).build();

        when(jsonService.parseUrl(eq(page1))).then(i -> IOUtils.fileAsJson("/remote/podcast/youtube/joueurdugrenier.json"));
        when(jsonService.parseUrl(eq(page2))).then(i -> IOUtils.fileAsJson("/remote/podcast/youtube/joueurdugrenier.2.json"));
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/youtube/joueurdugrenier.html"));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(87);
    }


    @Test
    public void should_failed_during_fetch_API() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        when(api.getYoutube()).thenReturn("FOO");
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .items(HashSet.<Item>empty().toJavaSet())
                .build();

        when(htmlService.get(any(String.class))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"));
        when(jsonService.parseUrl(anyString())).thenReturn(None());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "http://foo.bar/com";
        /* When */
        Integer compatibility = youtubeUpdater.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        List<String> urls = List(
                "http://www.youtube.com/channel/foo",
                "http://www.youtube.com/user/foo",
                "http://gdata.youtube.com/feeds/api/playlists/foo"
        );
        /* When */
        Set<Integer> results = urls.map(youtubeUpdater::compatibility).distinct().toSet();

        /* Then */
        assertThat(results).contains(1);
    }
}
