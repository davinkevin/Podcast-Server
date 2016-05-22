package lan.dk.podcastserver.manager.worker.selector;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.manager.worker.downloader.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloaderSelectorTest {

    private DownloaderSelector downloaderSelector;

    @Mock DailyMotionCloudDownloader dailyMotionCloudDownloader;
    @Mock DailymotionDownloader dailymotionDownloader;
    @Mock HTTPDownloader httpDownloader;
    @Mock M3U8Downloader m3U8Downloader;
    @Mock ParleysDownloader parleysDownloader;
    @Mock RTMPDownloader rtmpDownloader;
    @Mock YoutubeDownloader youtubeDownloader;
    @Mock ApplicationContext applicationContext;
    private Set<Downloader> downloaders;

    @Before
    public void setUp() throws Exception {
        when(dailyMotionCloudDownloader.compatibility(anyString())).thenCallRealMethod();
        when(dailymotionDownloader.compatibility(anyString())).thenCallRealMethod();
        when(httpDownloader.compatibility(anyString())).thenCallRealMethod();
        when(m3U8Downloader.compatibility(anyString())).thenCallRealMethod();
        when(parleysDownloader.compatibility(anyString())).thenCallRealMethod();
        when(rtmpDownloader.compatibility(anyString())).thenCallRealMethod();
        when(youtubeDownloader.compatibility(anyString())).thenCallRealMethod();
        when(applicationContext.getBean(anyString(), eq(Downloader.class))).then(findBean());

        downloaders = Sets.newHashSet(dailyMotionCloudDownloader, dailymotionDownloader, httpDownloader, m3U8Downloader, parleysDownloader, rtmpDownloader, youtubeDownloader);
        downloaderSelector = new DownloaderSelector(applicationContext, downloaders);
    }

    private Answer<Downloader> findBean() {
        return i -> downloaders
                .stream()
                .filter(d -> StringUtils.equals(((String) i.getArguments()[0]), d.getClass().getSimpleName()))
                .findFirst()
                .orElse(DownloaderSelector.NO_OP_DOWNLOADER);
    }

    @Test
    public void should_return_an_HTTPDownloader () {
        /* When  */ Downloader updaterClass = downloaderSelector.of("http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/tnt/tnt1217/tnt1217.mp3");
        /* Then  */ assertThat(updaterClass).isEqualTo(httpDownloader);
    }

    @Test
    public void should_return_an_RTMPDownloader () {
        /* When  */ Downloader updaterClass = downloaderSelector.of("rtmp://ma.video.free.fr/video.mp4");
        /* Then  */ assertThat(updaterClass).isEqualTo(rtmpDownloader);
    }

    @Test
    public void should_return_an_DailyMotionCloudDownloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("http://cdn.dmcloud.net/route/hls/52f0ce9994a6f65ac1125958/5507e32e94739940f5d479f2/abs-1426578247.m3u8?auth=1741946475-1-ruuv9t9k-67c31693f9ecc4fd8d14a89329705ef1&cells=current");
        /* Then  */ assertThat(updaterClass).isEqualTo(dailyMotionCloudDownloader);
    }

    @Test
    public void should_return_an_YoutubeDownloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("https://www.youtube.com/watch?v=RKh4T3m-Qlk&feature=youtube_gdata");
        /* Then  */ assertThat(updaterClass).isEqualTo(youtubeDownloader);
    }

    @Test
    public void should_return_an_M3U8Downloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("http://us-cplus-aka.canal-plus.com/i/1503/17/nip_NIP_47464_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8");
        /* Then  */ assertThat(updaterClass).isEqualTo(m3U8Downloader);
    }

    @Test
    public void should_return_an_ParleysDownloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("http://www.parleys.com/play/54d78f6de4b0767f26dffb67");
        /* Then  */ assertThat(updaterClass).isEqualTo(parleysDownloader);
    }

    @Test
    public void should_return_a_DailymotionDownloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("http://www.dailymotion.com/video/xLif1aca");
        /* Then  */ assertThat(updaterClass).isEqualTo(dailymotionDownloader);
    }

    @Test
    public void should_reject_empty_url() {
        /* When */  assertThat(downloaderSelector.of("")).isEqualTo(DownloaderSelector.NO_OP_DOWNLOADER);
    }
}