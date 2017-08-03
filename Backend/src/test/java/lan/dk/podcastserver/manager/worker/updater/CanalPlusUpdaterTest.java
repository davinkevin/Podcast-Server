package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 16/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class CanalPlusUpdaterTest {

    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SignatureService signatureService;
    private @Mock Validator validator;
    private @Mock JdomService jdomService;
    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @Mock M3U8Service m3U8Service;
    private @InjectMocks CanalPlusUpdater canalPlusUpdater;
    private Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast
                .builder()
                .id(UUID.randomUUID())
                .url("http://www.canalplus.com/url/fake")
                .title("A Canal Plus Podcast")
                .items(Sets.newHashSet())
                .build();
    }

    @Test
    public void should_sign_with_podcast_as_front_tools() throws URISyntaxException, IOException {
        /* Given */
        podcast.setUrl("http://a.fake.url/front_tools/foo.html");
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
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/lepetitjournal.html"));
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
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(Option.none());
        when(signatureService.generateSignatureFromURL(eq(""))).thenReturn("aSignature");

        /* When */
        String signature = canalPlusUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_not_find_front_tools_elements_in_page() throws URISyntaxException, IOException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/lepetitjournal_without_loadVideoHistory.html"));
        when(signatureService.generateSignatureFromURL(eq(""))).thenReturn("aSignature");

        /* When */
        String signature = canalPlusUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("aSignature");
    }

    @Test
    public void should_get_items_from_podcast() throws IOException, URISyntaxException, JDOMException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/lepetitjournal.html"));
        when(htmlService.get(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6515&ztid=6112&nbPlusVideos0=1"))).then(i1 -> IOUtils.fileAsHtml("/remote/podcast/canalplus/lepetitjournal.front_tools.html"));
        when(m3U8Service.getM3U8UrlFormMultiStreamFile(any())).then(i -> "/a/fake/url/with/custom/" + UUID.randomUUID());
        prepareXmlBackend();

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(16).contains(Item.DEFAULT_ITEM);
    }

    @Test
    public void should_not_find_nbPlusVideos() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        podcast.setUrl("http://a.fake.url/front_tools/foo.html");
        prepareXmlBackend();

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_not_videos_if_front_tools_failed() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/lepetitjournal.html"));
        when(htmlService.get(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6515&ztid=6112&nbPlusVideos0=1"))).thenReturn(Option.none());

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_handle_podcast_in_tabs_view() throws IOException, URISyntaxException, JDOMException {
        /* Given */
        podcast.setUrl(podcast.getUrl() + "&tab=1-6");
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/page_with_tabs.html"));
        when(htmlService.get(eq("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=6130&ztid=6112&nbPlusVideos0=1&liste=5")))
                .thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/page_with_tabs_front_tools.html"));
        when(jdomService.parse(anyString())).thenReturn(Option.none());

        /* When */
        Set<Item> items = canalPlusUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(1);
        verify(jdomService, times(16)).parse(anyString());
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
            when(jdomService.parse(eq(String.format(url, id)))).then(i -> IOUtils.fileAsXml(String.format(uri, id)));
        }
        when(jdomService.parse(eq(String.format(url, "1344688")))).thenReturn(Option.none());
    }
}
