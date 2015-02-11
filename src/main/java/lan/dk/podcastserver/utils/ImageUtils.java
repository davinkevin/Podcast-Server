package lan.dk.podcastserver.utils;

import lan.dk.podcastserver.entity.Cover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;


public class ImageUtils {

    private static Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    public static Cover getCoverFromURL (URL url) throws IOException {

        Cover cover = new Cover(url.toString());
        ImageInputStream imageInputStream;
        try (InputStream urlInputStream = URLUtils.getStreamWithTimeOut(cover.getUrl(), 5000).getInputStream() ){
            imageInputStream = ImageIO.createImageInputStream(urlInputStream);
        } catch (IOException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.debug("Error during creation of ImageInputStream", e);
            return null;
        }
        try {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInputStream);
                    cover.setHeight(reader.getHeight(0));
                    cover.setWidth(reader.getWidth(0));
                } catch (IOException e) {
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    logger.debug("Erreur HTTP : " + e.getMessage());
                    throw e;
                } finally {
                    reader.dispose();
                }
            }
        } finally {
            if (imageInputStream != null) try {
                imageInputStream.close();
            } catch (IOException e) {
                //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.debug("Erreur HTTP : " + e.getMessage());
                throw e;
            }
        }
        return cover;
    }
}
