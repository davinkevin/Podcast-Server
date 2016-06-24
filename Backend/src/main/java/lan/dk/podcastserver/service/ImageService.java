package lan.dk.podcastserver.service;


import lan.dk.podcastserver.entity.Cover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ImageService {

    final UrlService urlService;

    public Cover getCoverFromURL(String url) {
        if (StringUtils.isEmpty(url))
            return null;

        try {
            return getCoverFromURL(new URL(url));
        } catch (IOException e) {
            log.error("Error during fetching Cover information for {}", url);
            return null;
        }
    }

    public Cover getCoverFromURL (URL url) throws IOException {
        Cover cover;

        try (InputStream urlInputStream = urlService.getConnectionWithTimeOut(url.toString(), 5000).getInputStream() ){
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(urlInputStream);
            final BufferedImage image = ImageIO.read(imageInputStream);
            cover = Cover
                    .builder()
                        .url(url.toString())
                        .width(image.getWidth())
                        .height(image.getHeight())
                    .build();
        }

        return cover;
    }
}
