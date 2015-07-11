package lan.dk.podcastserver.service;


import lan.dk.podcastserver.entity.Cover;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Service
public class ImageService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UrlService urlService;

    @Autowired
    public ImageService(UrlService urlService) {
        this.urlService = urlService;
    }

    public Cover getCoverFromURL(String url) throws IOException {
        if (StringUtils.isEmpty(url))
            return null;

        return getCoverFromURL(new URL(url));
    }

    public Cover getCoverFromURL (URL url) throws IOException {
        Cover cover = new Cover(url.toString());

        try (InputStream urlInputStream = urlService.getConnectionWithTimeOut(cover.getUrl(), 5000).getInputStream() ){
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(urlInputStream);
            final BufferedImage image = ImageIO.read(imageInputStream);
            cover.setWidth(image.getWidth());
            cover.setHeight(image.getHeight());
        } catch (IOException e) {
            logger.debug("Error during creation of inputStream for {}", cover.getUrl());
        }
        return cover;
    }
}
