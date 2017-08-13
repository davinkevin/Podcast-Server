package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.vavr.API.None;
import static io.vavr.API.Option;
import static lan.dk.podcastserver.service.MimeTypeService.TikaProbeContentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class MimeTypeServiceTest {

    private @Mock TikaProbeContentType tikaProbeContentType;
    private @InjectMocks MimeTypeService mimeTypeService;

    @Test
    public void should_get_mimeType_if_no_extension() {
        /* When */  String mimeType = mimeTypeService.getMimeType("");
        /* Then */  assertThat(mimeType).isEqualTo("application/octet-stream");
    }

    @Test
    public void should_get_mimetype_for_known_extension() {
        /* When */  String mimeType = mimeTypeService.getMimeType("webm");
        /* Then */  assertThat(mimeType).isEqualTo("video/webm");
    }

    @Test
    public void should_get_mimetype_for_unknown_extension() {
        /* When */  String mimeType = mimeTypeService.getMimeType("txt");
        /* Then */  assertThat(mimeType).isEqualTo("unknown/txt");
    }

    @Test
    public void should_get_extension_by_mimeType() {
        /* Given */
        Item item = new Item().setMimeType("audio/mp3");

        /* When */ String extension = mimeTypeService.getExtension(item);
        /* Then */ assertThat(extension).isEqualTo(".mp3");
    }

    @Test
    public void should_get_extension_by_Youtube() {
        /* Given */
        Item item = new Item().setPodcast(new Podcast().setType("Youtube")).setUrl("http://fake.com/foo/bar");

        /* When */ String extension = mimeTypeService.getExtension(item);
        /* Then */ assertThat(extension).isEqualTo(".mp4");
    }

    @Test
    public void should_get_extension_by_url() {
        /* Given */
        Item item = new Item()
                .setPodcast(new Podcast().setType("Other"))
                .setUrl("http://fake.com/foo/bar.mp4a");

        /* When */ String extension = mimeTypeService.getExtension(item);
        /* Then */ assertThat(extension).isEqualTo(".mp4a");
    }

    @Test
    public void should_get_mimeType_with_probeContentType() throws URISyntaxException, IOException {
        /* Given */
        Path path = Paths.get(MimeTypeServiceTest.class.getResource("/remote/podcast/plain.text.txt").toURI());
        when(tikaProbeContentType.probeContentType(any(Path.class))).thenReturn(Option("text/plain"));

        /* When */ String type = mimeTypeService.probeContentType(path);
        /* Then */ assertThat(type).isEqualTo("text/plain");
    }

    @Test
    public void should_get_mimeType_with_tika() throws URISyntaxException, IOException {
        /* Given */
        Path file = Paths.get("/", "tmp", "foo");
        when(tikaProbeContentType.probeContentType(any(Path.class))).thenReturn(Option("Foo/bar"));

        /* When */ String type = mimeTypeService.probeContentType(file);
        /* Then */ assertThat(type).isEqualTo("Foo/bar");
    }

    @Test
    public void should_find_mimeType_from_inline_map() throws IOException {
        /* Given */
        Path file = Paths.get("/", "tmp", "foo");
        when(tikaProbeContentType.probeContentType(any(Path.class))).thenReturn(None());

        /* When */
        String contentType = mimeTypeService.probeContentType(file);

        /* Then */
        assertThat(contentType).isEqualTo("application/octet-stream");
    }
}
