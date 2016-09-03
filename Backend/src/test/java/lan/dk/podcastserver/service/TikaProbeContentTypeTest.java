package lan.dk.podcastserver.service;

import javaslang.control.Option;
import org.apache.tika.Tika;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static lan.dk.podcastserver.service.MimeTypeService.TikaProbeContentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 05/04/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class TikaProbeContentTypeTest {

    @Mock Tika tika;
    @InjectMocks TikaProbeContentType tikaProbeContentType;

    @Before
    public void beforeEach() {
        tikaProbeContentType = new TikaProbeContentType(tika);
    }

    @Test
    public void should_detect_content_type() throws IOException {
        /* Given */
        when(tika.detect(any(File.class))).thenReturn("Foo/bar");

        /* When */
        Option<String> mimeType = tikaProbeContentType.probeContentType(Paths.get("/", "tmp", "Bar"));

        /* Then */
        assertThat(mimeType).contains("Foo/bar");
    }
    
    @Test
    public void should_reject_because_exception() throws IOException {
        /* Given */
        doThrow(IOException.class).when(tika).detect(any(File.class));

        /* When */
        Option<String> mimeType = tikaProbeContentType.probeContentType(Paths.get("/", "tmp", "Bar"));

        /* Then */
        assertThat(mimeType).isEmpty();
    }

}