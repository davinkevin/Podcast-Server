package lan.dk.podcastserver.manager.selector;

import io.vavr.collection.HashSet;
import lan.dk.podcastserver.manager.downloader.*;
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

    private @Mock HTTPDownloader httpDownloader;
    private @Mock M3U8Downloader m3U8Downloader;
    private @Mock RTMPDownloader rtmpDownloader;
    private @Mock YoutubeDownloader youtubeDownloader;
    private @Mock ApplicationContext applicationContext;
    private Set<Downloader> downloaders;

    @Before
    public void setUp() {
        when(httpDownloader.compatibility(anyString())).thenCallRealMethod();
        when(m3U8Downloader.compatibility(anyString())).thenCallRealMethod();
        when(rtmpDownloader.compatibility(anyString())).thenCallRealMethod();
        when(youtubeDownloader.compatibility(anyString())).thenCallRealMethod();
        when(applicationContext.getBean(anyString(), eq(Downloader.class))).then(findBean());

        downloaders = HashSet.<Downloader>of(httpDownloader, m3U8Downloader, rtmpDownloader, youtubeDownloader).toJavaSet();
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
    public void should_return_an_YoutubeDownloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("https://www.youtube.com/watch?v=RKh4T3m-Qlk&feature=youtube_gdata");
        /* Then  */ assertThat(updaterClass).isEqualTo(youtubeDownloader);
    }

    @Test
    public void should_return_a_M3U8Downloader() {
        /* When  */ Downloader updaterClass = downloaderSelector.of("http://foo.bar.com/a/path/with/file.m3u8");
        /* Then  */ assertThat(updaterClass).isEqualTo(m3U8Downloader);
    }

    @Test
    public void should_reject_empty_url() {
        /* When */  assertThat(downloaderSelector.of("")).isEqualTo(DownloaderSelector.NO_OP_DOWNLOADER);
    }
}
