package lan.dk.podcastserver.manager.worker.sixplay;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.M3U8Service;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by kevin on 10/02/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class SixPlayExtractorTest {

    private static final String ITEM_PHYSICAL_URL = "https://lbv2.cdn.m6web.fr/v1/resource/s/usp/mb_sd3/d/a/6/Scenes-de-menages_c11887179_Episodes-du-09-fe/Scenes-de-menages_c11887179_Episodes-du-09-fe_unpnp.ism/Manifest.m3u8?expiration=1518300931&scheme=https&groups%5B0%5D=m6web&customerName=m6web&token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MTgyNjQ5MzAsIm5iZiI6MTUxODI2NDkzMCwiZXhwIjoxNTE4MzAwOTMxLCJyX2hhc2giOiJkN2I2M2ExMmRlMmVmOTYxY2Y4NTk2NjU1OTE0NWI0YjUxMmM2ZjFjIn0.lCbr5KE3dX6X7Ic3c8s9SYsJiZcDCYszmo-cGJYoG28";
    private static final String REAL_URL = "https://cdn-m6web.akamaized.net/prime/vod/protected/d/a/6/Scenes-de-menages_c11887179_Episodes-du-09-fe/Scenes-de-menages_c11887179_Episodes-du-09-fe_sd3.m3u8";
    private static final String ITEM_6PLAY_URL = "https://pc.middleware.6play.fr/6play/v2/platforms/m6group_web/services/6play/videos/clip_11887179?with=clips&csa=5";
    private static final String PLAYLIST_6PLAY_URL = "https://pc.middleware.6play.fr/6play/v2/platforms/m6group_web/services/6play/videos/playlist_2372?with=clips&csa=5";

    private @Mock JsonService jsonService;
    private @Mock M3U8Service m3U8Service;
    private @Mock UrlService urlService;
    private @InjectMocks SixPlayExtractor extractor;

    private Item itemClip;
    private Item itemPlaylist;

    @Before
    public void beforeEach() {
        itemClip = Item.builder()
                .url("https://www.6play.fr/scenes-de-menages-p_829/episodes-du-09-fevrier-a-2025-c_11887179")
                .build();
        itemPlaylist = Item.builder()
                .url("https://www.6play.fr/scenes-de-menages-p_829/psychologie-du-couple-p_2372")
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_extract_real_url_for_clip() throws UnirestException {
        /* GIVEN */
        when(jsonService.parseUrl(ITEM_6PLAY_URL)).thenReturn(IOUtils.fileAsJson(of("c_11887179.json")));
        when(urlService.getRealURL(ITEM_PHYSICAL_URL)).thenReturn(REAL_URL);

        GetRequest request = mock(GetRequest.class);
        HttpResponse response = mock(HttpResponse.class);
        when(urlService.get(REAL_URL)).thenReturn(request);
        when(request.header(any(), any())).thenReturn(request);
        when(request.asString()).thenReturn(response);
        when(response.getRawBody()).thenReturn(IOUtils.fileAsStream(of("118817179.manifest.m3u8")));

        when(m3U8Service.findBestQuality(any())).thenCallRealMethod();
        when(urlService.addDomainIfRelative(any(), any())).thenCallRealMethod();

        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(itemClip);

        /* THEN  */
        assertThat(downloadingItem.getItem()).isSameAs(itemClip);
        assertThat(downloadingItem.getUrls()).containsOnly("https://cdn-m6web.akamaized.net/prime/vod/protected/d/a/6/Scenes-de-menages_c11887179_Episodes-du-09-fe/Scenes-de-menages_c11887179_Episodes-du-09-fe_sd3.mp4.m3u8");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_extract_urls_from_playlist() {
        /* GIVEN */
        when(jsonService.parseUrl(PLAYLIST_6PLAY_URL)).thenReturn(IOUtils.fileAsJson(of("p_2372.json")));
        /* WHEN  */
        DownloadingItem downloadingItem = extractor.extract(itemPlaylist);
        /* THEN  */
        assertThat(downloadingItem.getItem()).isSameAs(itemPlaylist);
        assertThat(downloadingItem.getUrls()).containsOnly(
                "https://lb.cdn.m6web.fr/p/s/5/db1b61589e0e02e10ceb781e66c5bad7/5a7f7550/u/videonum/4/8/8/scenesdemenages__POUVOIR-PSY__20170412__58edf15f83a5a_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/c02f710dcafc76d92c28bac3dec99e8b/5a7f7550/u/videonum/2/5/2/scenesdemenages__ESTEVE-PSYCHOLOGUE__20170412__58edf33f9dcd7_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/5efe681c11a35ae2a63dc4c307151afd/5a7f7550/u/videonum/a/3/8/scenesdemenages__PSYCHOLOGIE__20170412__58edf19baa5a2_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/3ff48a5a18fc4f6572fec98aa0c0eba9/5a7f7550/u/videonum/3/3/c/scenesdemenages__PSY-CAUSE__20170412__58edf1d8d5cea_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/013e806c6252737caddd75702a0b842e/5a7f7550/u/videonum/d/5/2/scenesdemenages__PRETEXTE-PSY__20170412__58edf213ecb06_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/e2894d22c050a2d9adff246955af6fcd/5a7f7550/u/videonum/f/6/c/scenesdemenages__PSYTIF__20170412__58edf2500e8ab_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/0c8e3a159957d9a0049cc85695a25a15/5a7f7550/u/videonum/8/9/c/scenesdemenages__LHEURE-DU-PSY__20170412__58edf28b3a3f9_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/341d8d0e209fb4c05e6757a362eafa07/5a7f7550/u/videonum/d/2/4/scenesdemenages__RUBRIQUE-PSYCHO__20170412__58edf2c760ea9_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/805f0b0140725c6c7672a4bdd0e7822d/5a7f7550/u/videonum/2/d/6/scenesdemenages__PSYCHANALYSE__20170412__58edf30386411_hq.mp4",
                "https://lb.cdn.m6web.fr/p/s/5/514dbd763ad4e6225eab21cd5e84e99f/5a7f7550/u/videonum/5/9/e/scenesdemenages__DE-PSY-A-TREPAS__20170412__58edf340a0bf3_hq.mp4"
        );
    }

    @Test
    public void should_be_only_compatible_with_6play_url() {
        assertThat(extractor.compatibility(null)).isGreaterThan(1);
        assertThat(extractor.compatibility("foo")).isGreaterThan(1);
        assertThat(extractor.compatibility("http://www.6play.fr/test")).isEqualTo(1);
    }

    private String of(String filename) {
        return "/remote/podcast/6play/" + filename;
    }

}