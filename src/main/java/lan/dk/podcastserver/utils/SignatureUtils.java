package lan.dk.podcastserver.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by kevin on 24/01/15.
 */
public class SignatureUtils {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    private static final Logger logger = LoggerFactory.getLogger(SignatureUtils.class);

    public static String generateSignatureFromURL(String urlAsString) {
        try {
            URL url = new URL(urlAsString);

            URLConnection conn = url.openConnection();
            
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            
            return DigestUtils.md5Hex(conn.getInputStream());
        } catch (IOException e) {
            logger.error("Error during signature of podcast at url {}", urlAsString, e);
        }
        return "";
    }
    
    public static String generateMD5SignatureFromDOM(String html){
        return DigestUtils.md5Hex(html);
    }
}
