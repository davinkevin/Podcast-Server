package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 16/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class CanalPlusUpdaterTest {

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock JdomService jdomService;
    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @Mock UrlService urlService;
    @InjectMocks CanalPlusUpdater canalPlusUpdater;
    Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast
                .builder()
                    .id(1)
                    .url("http://www.canalplus.com/url/fake")
                    .title("A Canal Plus Podcast")
                    .items(Sets.newHashSet())
                .build();
    }

    @Test
    public void should_sign_with_podcast_as_front_tools() throws URISyntaxException, IOException {
        /* Given */
        podcast.setUrl("http://a.fake.url/front_tools/foo.html");
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.html"));
        when(signatureService.generateSignatureFromURL(anyString())).thenReturn("aSignature");

        /* When */
        String signature = canalPlusUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
        verify(signatureService, only()).generateSignatureFromURL(eq(podcast.getUrl()));
    }

    @Test
    public void should_sign_the_podcast() throws URISyntaxException, IOException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.html"));
        when(signatureService.generateSignatureFromURL(anyString())).thenReturn("aSignature");

        /* When */
        String signature = canalPlusUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
        verify(signatureService, only()).generateSignatureFromURL(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6515&ztid=6112&nbPlusVideos0=1"));
    }

    @Test
    public void should_reject_signature_with_empty() throws URISyntaxException, IOException {
        /* Given */
        doThrow(IOException.class).when(htmlService).get(eq(podcast.getUrl()));
        when(signatureService.generateSignatureFromURL(eq(""))).thenReturn("aSignature");

        /* When */
        String signature = canalPlusUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_not_find_front_tools_elements_in_page() throws URISyntaxException, IOException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal_without_loadVideoHistory.html"));
        when(signatureService.generateSignatureFromURL(eq(""))).thenReturn("aSignature");

        /* When */
        String signature = canalPlusUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_get_items_from_podcast() throws IOException, URISyntaxException, JDOMException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.html"));
        when(htmlService.get(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6515&ztid=6112&nbPlusVideos0=1"))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.front_tools.html"));
        prepareXmlBackend();

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(16)
            .contains(Item.DEFAULT_ITEM);
    }

    @Test
    public void should_not_find_nbPlusVideos() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        podcast.setUrl("http://a.fake.url/front_tools/foo.html");
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.html"));
        when(htmlService.get(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6515&ztid=6112&nbPlusVideos0=1"))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.front_tools.html"));
        prepareXmlBackend();

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_not_videos_if_front_tools_failed() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).then(readHtmlFromFile("/remote/podcast/canalplus/lepetitjournal.html"));
        doThrow(IOException.class).when(htmlService).get(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6515&ztid=6112&nbPlusVideos0=1"));

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_have_a_type() {
        assertThat(canalPlusUpdater.type().key()).isEqualTo("CanalPlus");
        assertThat(canalPlusUpdater.type().name()).isEqualTo("Canal+");
    }

    private void prepareXmlBackend() throws JDOMException, IOException, URISyntaxException {
        String url = "http://service.canal-plus.com/video/rest/getVideos/cplus/%s";
        String uri = "/remote/podcast/canalplus/lepetitjournal.%s.xml";

        for (String id : Arrays.asList(/*"1344688", */"1345586", "1345804", "1345857", "1345867", "1347250", "1347728", "1348127", "1348490", "1348841", "1348993", "1349772", "1350194", "1350642", "1351047", "1351482")) {
            when(jdomService.parse(eq(String.format(url, id)))).then(readXmlFromFile(String.format(uri, id)));
        }
        doThrow(IOException.class).when(jdomService).parse(eq(String.format(url, "1344688")));
    }

    private Answer<Document> readHtmlFromFile(String s) throws URISyntaxException {
        Path path = Paths.get(CanalPlusUpdaterTest.class.getResource(s).toURI());
        return i -> Jsoup.parse(path.toFile(), "UTF-8", "http://www.canalplus.fr/");
    }

    private Answer<org.jdom2.Document> readXmlFromFile(String s) throws URISyntaxException {
        return i -> new SAXBuilder().build(Paths.get(CanalPlusUpdaterTest.class.getResource(s).toURI()).toFile());
    }
}