package lan.dk.podcastserver.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Created by kevin on 22/07/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class M3U8ServiceTest {

    private M3U8Service m3U8Service;

    @Before
    public void beforeEach() {
        m3U8Service = new M3U8Service();
    }

    @Test
    public void should_select_best_audio_video_url() {
        /* Given */
        InputStream m3u8FileStream = M3U8ServiceTest.class.getResourceAsStream("/remote/podcast/tf1replay/13184238.m3u8");

        /* When */
        Optional<String> bestQuality = m3U8Service.findBestQuality(m3u8FileStream);

        /* Then */
        assertThat(bestQuality).isPresent();
        assertThat(bestQuality).hasValue("13184238-audio%3D64000-video%3D2500299.m3u8?vk=MTMxODQyMzgubTN1OA==&st=D79oBJFWiP__EA4uMJAejg&e=1469146222&t=1469135422&min_bitrate=");
    }

    @Test
    public void should_not_select_video_url() throws IOException {
        /* Given */
        InputStream is = mock(InputStream.class);
        doThrow(IOException.class).when(is).read();

        /* When */
        Optional<String> bestQuality = m3U8Service.findBestQuality(is);

        /* Then */
        assertThat(bestQuality).isEmpty();
    }
}