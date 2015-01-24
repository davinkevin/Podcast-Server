package lan.dk.podcastserver.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Created by kevin on 24/01/15.
 */
public class SignatureUtils {


    private static final Logger logger = LoggerFactory.getLogger(SignatureUtils.class);
    
    public static String generateSignatureFromURL(String urlAsString) {
        try {
            URL url = new URL(urlAsString);
            return DigestUtils.md5Hex(url.openStream());
        } catch (IOException e) {
            logger.error("Error during signature of podcast at url {}", urlAsString, e);
        }
        return "";
    }
    
    public static String generateMD5SignatureFromDOM(String html){
        return DigestUtils.md5Hex(html);
    }
}
