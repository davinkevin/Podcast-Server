package lan.dk.podcastserver.service.factory;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import lan.dk.podcastserver.service.UrlService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 22/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class WGetHelperTest {

    @Mock UrlService urlService;
    @InjectMocks WGetHelper wGetHelper;

    @Test
    public void should_do_di() {
        assertThat(wGetHelper.urlService).isNotNull();
    }

    @Test
    public void should_generate_a_parser() throws MalformedURLException {
        /* Given */
        String url = "http://www.youtube.com/foo/bar";

        /* When */
        VGetParser vParser = wGetHelper.vParser(url);

        /* Then */
        assertThat(vParser).isNotNull().isInstanceOf(VGetParser.class);
    }

    @Test
    public void should_get_info() throws MalformedURLException {
        /* Given */
        VideoInfo videoInfo = new VideoInfo(new URL("http://www.youtube.com/foo/bar"));

        /* When */
        VGet vget = wGetHelper.vGet(videoInfo);

        /* Then */
        assertThat(vget).isNotNull().isInstanceOf(VGet.class);
    }
}