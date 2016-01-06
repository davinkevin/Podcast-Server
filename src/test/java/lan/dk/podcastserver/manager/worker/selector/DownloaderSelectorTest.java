package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.downloader.*;
import lan.dk.podcastserver.manager.worker.selector.download.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DownloaderSelectorTest {

    Set<DownloaderCompatibility> downloaderCompatibilities = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        downloaderCompatibilities.add(new HTTPDownloaderCompatibility());
        downloaderCompatibilities.add(new RTMPDownloaderCompatibility());
        downloaderCompatibilities.add(new DailyMotionCloudDownloaderCompatibility());
        downloaderCompatibilities.add(new YoutubeDownloaderCompatibility());
        downloaderCompatibilities.add(new M3U8DownloaderCompatibility());
        downloaderCompatibilities.add(new ParleysDownloaderCompatibility());
    }

    @Test
    public void should_return_an_HTTPDownloader () {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When  */ Class updaterClass = downloaderSelector.of("http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/tnt/tnt1217/tnt1217.mp3");
        /* Then  */ assertThat(updaterClass).isEqualTo(HTTPDownloader.class);
    }

    @Test
    public void should_return_an_RTMPDownloader () {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When  */ Class updaterClass = downloaderSelector.of("rtmp://ma.video.free.fr/video.mp4");
        /* Then  */ assertThat(updaterClass).isEqualTo(RTMPDownloader.class);
    }

    @Test
    public void should_return_an_DailyMotionCloudDownloader() {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When  */ Class updaterClass = downloaderSelector.of("http://cdn.dmcloud.net/route/hls/52f0ce9994a6f65ac1125958/5507e32e94739940f5d479f2/abs-1426578247.m3u8?auth=1741946475-1-ruuv9t9k-67c31693f9ecc4fd8d14a89329705ef1&cells=current");
        /* Then  */ assertThat(updaterClass).isEqualTo(DailyMotionCloudDownloader.class);
    }

    @Test
    public void should_return_an_YoutubeDownloader() {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When  */ Class updaterClass = downloaderSelector.of("https://www.youtube.com/watch?v=RKh4T3m-Qlk&feature=youtube_gdata");
        /* Then  */ assertThat(updaterClass).isEqualTo(YoutubeDownloader.class);
    }

    @Test
    public void should_return_an_M3U8Downloader() {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When  */ Class updaterClass = downloaderSelector.of("http://us-cplus-aka.canal-plus.com/i/1503/17/nip_NIP_47464_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8");
        /* Then  */ assertThat(updaterClass).isEqualTo(M3U8Downloader.class);
    }

    @Test
    public void should_return_an_ParleysDownloader() {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When  */ Class updaterClass = downloaderSelector.of("http://www.parleys.com/play/54d78f6de4b0767f26dffb67");
        /* Then  */ assertThat(updaterClass).isEqualTo(ParleysDownloader.class);
    }

    @Test(expected = RuntimeException.class)
    public void should_reject_empty_url() {
        /* Given */ DownloaderSelector downloaderSelector = new DownloaderSelector().setDownloaderCompatibilities(downloaderCompatibilities);
        /* When */  downloaderSelector.of("");
    }
}