package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
@Service
public class MimeTypeService {

    public String getMimeType(String extension) {
        return MimeTypeUtils.getMimeType(extension);
    }

    public String getExtension(Item item) {
        return MimeTypeUtils.getExtension(item);
    }

    // https://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
    public String probeContentType(Path file) throws IOException {
        return MimeTypeUtils.probeContentType(file);
    }
}
