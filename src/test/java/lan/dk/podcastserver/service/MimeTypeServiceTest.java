package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
public class MimeTypeServiceTest {

    MimeTypeService mimeTypeService;

    @Before
    public void beforeEach() {
        mimeTypeService = new MimeTypeService();
    }

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

        /* When */ String type = mimeTypeService.probeContentType(path);
        /* Then */ assertThat(type).isEqualTo("text/plain");
    }
    
    @Test
    public void should_find_mimeType_from_inline_map() {
        /* Given */
        Path file = Paths.get("/", "root", "foo");

        /* When */
        String contentType = mimeTypeService.probeContentType(file);

        /* Then */
        assertThat(contentType).isEqualTo("application/octet-stream");
    }
}