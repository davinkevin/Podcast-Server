package lan.dk.podcastserver.service.factory;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import lan.dk.podcastserver.service.UrlService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by kevin on 22/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class WGetFactoryTest {

    @Mock UrlService urlService;
    @InjectMocks WGetFactory wGetFactory;

    @Test
    public void should_do_di() {
        assertThat(wGetFactory.urlService).isNotNull();
    }

    @Test
    public void should_generate_a_parser() throws MalformedURLException {
        /* Given */
        String url = "http://www.youtube.com/foo/bar";

        /* When */
        VGetParser vParser = wGetFactory.parser(url);

        /* Then */
        assertThat(vParser).isNotNull().isInstanceOf(VGetParser.class);
    }

    @Test
    public void should_get_info() throws MalformedURLException {
        /* Given */
        VideoInfo videoInfo = new VideoInfo(new URL("http://www.youtube.com/foo/bar"));

        /* When */
        VGet vget = wGetFactory.newVGet(videoInfo);

        /* Then */
        assertThat(vget).isNotNull().isInstanceOf(VGet.class);
    }

    @Test
    public void should_parse_with_wget() {
        /* Given */
        DownloadInfo downloadInfo = mock(DownloadInfo.class);
        Path path = Paths.get("/tmp/afile.tmp");

        /* When */
        WGet wGet = wGetFactory.newWGet(downloadInfo, path.toFile());

        /* Then */
        assertThat(wGet).isNotNull().isInstanceOf(WGet.class);
    }
    
    @Test
    public void should_get_wget_download_info() throws MalformedURLException {
        /* Given */
        String url = "http://www.youtube.com/user/cauetofficiel";

        /* When */
        DownloadInfo downloadInfo = wGetFactory.newDownloadInfo(url);

        /* Then */
        assertThat(downloadInfo).isNotNull().isInstanceOf(DownloadInfo.class);
    }

}