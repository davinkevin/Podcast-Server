package lan.dk.podcastserver.service;


import lan.dk.podcastserver.entity.Cover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    final UrlService urlService;

    public Cover getCoverFromURL(String url) {
        if (StringUtils.isEmpty(url))
            return Cover.DEFAULT_COVER;

        try (InputStream urlInputStream = urlService.asStream(url) ){
            final BufferedImage image = ImageIO.read(ImageIO.createImageInputStream(urlInputStream));
            return Cover
                    .builder()
                        .url(url)
                        .width(image.getWidth())
                        .height(image.getHeight())
                    .build();
        } catch (IOException e) {
            log.error("Error during fetching Cover information for {}", url);
            return Cover.DEFAULT_COVER;
        }
    }

}
