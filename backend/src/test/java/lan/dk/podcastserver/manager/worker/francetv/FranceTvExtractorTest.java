package lan.dk.podcastserver.manager.worker.francetv;

import com.github.davinkevin.podcastserver.service.HtmlService;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.vavr.API.None;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 24/12/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class FranceTvExtractorTest {

    private static final String ITEM_URL = "https://www.france.tv/spectacles-et-culture/emissions-culturelles/14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.html";
    private static final String REAL_URL = "https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais";

    private @Mock JsonService jsonService;
    private @Mock HtmlService htmlService;
    private @InjectMocks
    FranceTvExtractor extractor;

    private Item item;

    @Before
    public void beforeEach() {
        item = Item.builder()
                .title("Secrets d'histoire - Jeanne d'Arc, au nom de Dieu")
                .url(ITEM_URL)
                .build();
    }

    @Test
    public void should_get_url_for_given_item() {
        /* GIVEN */
        when(htmlService.get(ITEM_URL)).then(i -> IOUtils.fileAsHtml(from("200015-le-divorce-satyrique-fondateur-de-la-legende-noire-de-la-reine-margot.html")));
        when(jsonService.parseUrl(fromCatalog("006a3008-8f95-52d3-be47-c15cf3640542"))).then(i -> IOUtils.fileAsJson(from("006a3008-8f95-52d3-be47-c15cf3640542.json")));

        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly(REAL_URL);
        assertThat(downloadingItem.getItem()).isSameAs(item);
        assertThat(downloadingItem.getFilename()).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4");
        verify(htmlService, times(1)).get(ITEM_URL);
        verify(jsonService, times(1)).parseUrl(anyString());
    }


    @Test
    public void should_use_m3u8_url_as_backup_if_no_hsl_stream() {
        /* GIVEN */
        when(htmlService.get(ITEM_URL)).then(i -> IOUtils.fileAsHtml(from("200015-le-divorce-satyrique-fondateur-de-la-legende-noire-de-la-reine-margot.html")));
        when(jsonService.parseUrl(fromCatalog("006a3008-8f95-52d3-be47-c15cf3640542"))).then(i -> IOUtils.fileAsJson(from("006a3008-8f95-52d3-be47-c15cf3640542_without_hls_stream.json")));

        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly("https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8");
        assertThat(downloadingItem.getItem()).isSameAs(item);
        assertThat(downloadingItem.getFilename()).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4");
        verify(htmlService, times(1)).get(ITEM_URL);
        verify(jsonService, times(1)).parseUrl(anyString());
    }

    @Test
    public void should_use_first_m3u8_stream_if_two_formats_are_not_found() {
        /* GIVEN */
        when(htmlService.get(ITEM_URL)).then(i -> IOUtils.fileAsHtml(from("200015-le-divorce-satyrique-fondateur-de-la-legende-noire-de-la-reine-margot.html")));
        when(jsonService.parseUrl(fromCatalog("006a3008-8f95-52d3-be47-c15cf3640542"))).then(i -> IOUtils.fileAsJson(from("006a3008-8f95-52d3-be47-c15cf3640542_without_hls_and_official_m3u8.json")));

        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly("https://fake.url.com/index.m3u8");
        assertThat(downloadingItem.getItem()).isSameAs(item);
        assertThat(downloadingItem.getFilename()).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4");
        verify(htmlService, times(1)).get(ITEM_URL);
        verify(jsonService, times(1)).parseUrl(anyString());
    }

    @Test
    public void should_use_not_secure_url_if_secured_not_found() {
        /* GIVEN */
        when(htmlService.get(ITEM_URL)).then(i -> IOUtils.fileAsHtml(from("200015-le-divorce-satyrique-fondateur-de-la-legende-noire-de-la-reine-margot.html")));
        when(jsonService.parseUrl(fromCatalog("006a3008-8f95-52d3-be47-c15cf3640542"))).then(i -> IOUtils.fileAsJson(from("006a3008-8f95-52d3-be47-c15cf3640542_without_secured_url.json")));

        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly("http://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais");
        assertThat(downloadingItem.getItem()).isSameAs(item);
        assertThat(downloadingItem.getFilename()).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4");
        verify(htmlService, times(1)).get(ITEM_URL);
        verify(jsonService, times(1)).parseUrl(anyString());
    }

    @Test
    public void should_throw_exception_if_no_url_found() {
        /* GIVEN */
        when(htmlService.get(ITEM_URL)).then(i -> IOUtils.fileAsHtml(from("200015-le-divorce-satyrique-fondateur-de-la-legende-noire-de-la-reine-margot.html")));
        when(jsonService.parseUrl(fromCatalog("006a3008-8f95-52d3-be47-c15cf3640542"))).then(i -> IOUtils.fileAsJson(from("006a3008-8f95-52d3-be47-c15cf3640542_without_videos.json")));

        /* WHEN  */
        assertThatThrownBy(() -> extractor.extract(item))
        /* THEN  */
            .isInstanceOf(RuntimeException.class)
            .hasMessageStartingWith("No video found in this FranceTvItem");
    }

    @Test
    public void should_throw_exception_if_can_t_find_url_at_all() {
        /* GIVEN */
        when(htmlService.get(ITEM_URL)).thenReturn(None());

        /* WHEN  */
        assertThatThrownBy(() -> extractor.extract(item))
        /* THEN  */
                .isInstanceOf(RuntimeException.class)
                .withFailMessage("Url not found for " + item.getUrl());
    }

    @Test
    public void should_be_compatible() {
        /* GIVEN */
        String url = "https://www.france.tv/foo/bar/toto";
        /* WHEN  */
        Integer compatibility = extractor.compatibility(url);
        /* THEN  */
        assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        /* GIVEN */
        String url = "https://www.france2.tv/foo/bar/toto";
        /* WHEN  */
        Integer compatibility = extractor.compatibility(url);
        /* THEN  */
        assertThat(compatibility).isGreaterThan(1);
    }

    private static String from(String s) {
        return "/remote/podcast/francetv/" + s;
    }

    private static String fromCatalog(String id) {
        return "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=" + id;
    }

}