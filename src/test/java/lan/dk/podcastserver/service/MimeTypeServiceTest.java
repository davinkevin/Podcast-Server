package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
public class MimeTypeServiceTest {

    @Test
    public void should_get_mimeType_if_no_extension() {
        /* Given */ MimeTypeService mimeTypeService = new MimeTypeService();
        /* When */  String mimeType = mimeTypeService.getMimeType("");
        /* Then */  assertThat(mimeType).isEqualTo("application/octet-stream");
    }

    @Test
    public void should_get_mimetype_for_known_extension() {
        /* Given */ MimeTypeService mimeTypeService = new MimeTypeService();
        /* When */  String mimeType = mimeTypeService.getMimeType("webm");
        /* Then */  assertThat(mimeType).isEqualTo("video/webm");
    }

    @Test
    public void should_get_mimetype_for_unknown_extension() {
        /* Given */ MimeTypeService mimeTypeService = new MimeTypeService();
        /* When */  String mimeType = mimeTypeService.getMimeType("txt");
        /* Then */  assertThat(mimeType).isEqualTo("unknown/txt");
    }

    @Test
    public void should_get_extension_by_mimeType() {
        /* Given */
        MimeTypeService mimeTypeService = new MimeTypeService();
        Item item = new Item().setMimeType("audio/mp3");

        /* When */ String extension = mimeTypeService.getExtension(item);
        /* Then */ assertThat(extension).isEqualTo(".mp3");
    }

    @Test
    public void should_get_extension_by_Youtube() {
        /* Given */
        MimeTypeService mimeTypeService = new MimeTypeService();
        Item item = new Item().setPodcast(new Podcast().setType("Youtube")).setUrl("http://fake.com/foo/bar");

        /* When */ String extension = mimeTypeService.getExtension(item);
        /* Then */ assertThat(extension).isEqualTo(".mp4");
    }

    @Test
    public void should_get_extension_by_url() {
        /* Given */
        MimeTypeService mimeTypeService = new MimeTypeService();
        Item item = new Item()
                .setPodcast(new Podcast().setType("Other"))
                .setUrl("http://fake.com/foo/bar.mp4a");

        /* When */ String extension = mimeTypeService.getExtension(item);
        /* Then */ assertThat(extension).isEqualTo(".mp4a");
    }

}