package lan.dk.podcastserver.manager.worker.jeuxvideocom;

import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static io.vavr.API.None;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 10/12/2015 for Podcast Server
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class JeuxVideoComUpdaterTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks
    JeuxVideoComUpdater jeuxVideoComUpdater;

    private static final Podcast CHRONIQUE_VIDEO = Podcast.builder()
            .title("Chronique Video HD")
            .url("http://www.jeuxvideo.com/chroniques-video.htm")
            .build();

    @Test
    public void should_sign_podcast() throws IOException, URISyntaxException {
        /* Given */
        configureHtmlServiceWith(CHRONIQUE_VIDEO.getUrl(), "/remote/podcast/JeuxVideoCom/chroniques-video.htm");
        when(signatureService.fromText(anyString())).thenReturn("aSignature");

        /* When */
        String signature = jeuxVideoComUpdater.signatureOf(CHRONIQUE_VIDEO);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_error_during_sign() throws IOException {
        /* Given */
        when(htmlService.get(eq(CHRONIQUE_VIDEO.getUrl()))).thenReturn(None());

        /* When */
        String signature = jeuxVideoComUpdater.signatureOf(CHRONIQUE_VIDEO);

        /* Then */
        assertThat(signature).isEqualTo("");
    }

    @Test
    public void should_get_items() throws IOException, URISyntaxException {
        /* Given */
        configureHtmlServiceWith(CHRONIQUE_VIDEO.getUrl(), "/remote/podcast/JeuxVideoCom/chroniques-video.htm");
        configureForAllPage("/remote/podcast/JeuxVideoCom/chroniques-video.htm");

        /* When */
        Set<Item> items = jeuxVideoComUpdater.getItems(CHRONIQUE_VIDEO);

        /* Then */
        assertThat(items).hasSize(42);
    }

    @Test
    public void should_return_empty_list_if_not_found() throws IOException {
        /* Given */
        when(htmlService.get(eq(CHRONIQUE_VIDEO.getUrl()))).thenReturn(None());

        /* When */
        Set<Item> items = jeuxVideoComUpdater.getItems(CHRONIQUE_VIDEO);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_get_items_with_exception() throws IOException, URISyntaxException {
        /* Given */
        configureHtmlServiceWith(CHRONIQUE_VIDEO.getUrl(), "/remote/podcast/JeuxVideoCom/chroniques-video.htm");
        configureForAllPage("/remote/podcast/JeuxVideoCom/chroniques-video.htm");
        when(htmlService.get(eq("http://www.jeuxvideo.com/videos/chroniques/452234/seul-face-aux-tenebres-le-rodeur-de-la-bibliotheque.htm"))).thenReturn(None());

        /* When */
        Set<Item> items = jeuxVideoComUpdater.getItems(CHRONIQUE_VIDEO);

        /* Then */
        assertThat(items)
                .hasSize(42)
                .contains(Item.DEFAULT_ITEM);
    }

    @Test
    public void should_be_of_type() {
        assertThat(jeuxVideoComUpdater.type().key()).isEqualTo("JeuxVideoCom");
        assertThat(jeuxVideoComUpdater.type().name()).isEqualTo("JeuxVideo.com");
    }

    private Answer<Option<Document>> readHtmlFromFileOptional(String s) throws URISyntaxException {
        return i -> IOUtils.fileAsHtml(s);
    }

    private void configureHtmlServiceWith(String url, String file) throws IOException, URISyntaxException {
        when(htmlService.get(eq(url))).then(readHtmlFromFileOptional(file));
    }

    private void configureForAllPage(String file) throws URISyntaxException, IOException {
        Path path = IOUtils.toPath(file).get();
        Document page = Jsoup.parse(path.toFile(), "UTF-8", "http://www.jeuxvideo.com");
        page.select("article")
                .stream()
                .map(e -> e.select("a").first().attr("href"))
                .forEach(url -> {
                    try {
                        configureHtmlServiceWith(JeuxVideoComUpdater.JEUXVIDEOCOM_HOST + url, "/remote/podcast/JeuxVideoCom/" + StringUtils.substringAfterLast(url, "/"));
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                });
    }

}
