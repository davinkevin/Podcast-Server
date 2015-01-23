package lan.dk.podcastserver.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static Logger logger = LoggerFactory.getLogger(DigestUtils.class);

    public static String generateMD5SignatureFromUrl(String url) {
        try {
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = new URL(url).openStream();
            DigestInputStream dis = new DigestInputStream(is, md);
            try {
                logger.debug("Beginning of digest on {}", url);
                while (dis.read() != -1);
                logger.debug("End of digest on {}", url);
            } finally {
                is.close();
                dis.close();
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();

            for (byte aDigest : digest) {
                sb.append(
                        Integer.toString((aDigest & 0xff) + 0x100, 16).substring(
                                1));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String generateMD5SignatureFromDOM(String html) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(html.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(Integer.toHexString(b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
