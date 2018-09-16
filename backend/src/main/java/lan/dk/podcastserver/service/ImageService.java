package lan.dk.podcastserver.service;


import com.github.davinkevin.podcastserver.service.UrlService;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.util.Objects;

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
            return null;
        }

        return Try.withResources(() -> urlService.asStream(url))
                .of(is -> ImageIO.read(ImageIO.createImageInputStream(is)))
                .onFailure(e -> log.error("Error during fetching Cover information for {}", url, e))
                .toOption()
                .filter(Objects::nonNull)
                .map(image -> Cover.builder().url(url).width(image.getWidth()).height(image.getHeight()).build())
                .getOrElse(() -> null);
    }

}
