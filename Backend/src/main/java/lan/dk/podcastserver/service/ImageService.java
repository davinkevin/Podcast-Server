package lan.dk.podcastserver.service;


import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static io.vavr.API.Try;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    final UrlService urlService;

    public Cover getCoverFromURL(String url) {
        if (StringUtils.isEmpty(url)) {
            return Cover.DEFAULT_COVER;
        }

        return Try.withResources(() -> urlService.asStream(url))
                .of(is -> ImageIO.read(ImageIO.createImageInputStream(is)))
                .map(image -> Cover.builder().url(url).width(image.getWidth()).height(image.getHeight()).build())
                .onFailure(e -> log.error("Error during fetching Cover information for {}", url, e))
                .getOrElse(() -> Cover.DEFAULT_COVER);
    }

}
