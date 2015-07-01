package lan.dk.podcastserver.service;


import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.utils.ImageUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@Service
public class ImageService {

    public Cover getCoverFromURL(String url) throws IOException {
        return ImageUtils.getCoverFromURL(url);
    }

    public Cover getCoverFromURL (URL url) throws IOException {
        return ImageUtils.getCoverFromURL(url);
    }
}
