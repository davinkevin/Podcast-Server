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
            logger.debug("Debut de la signature");
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = new URL(url).openStream();

            try {
                is = new DigestInputStream(is, md);

                int b;

                while ((b = is.read()) != -1) {
                    //logger.debug(".");
                    ;
                }
            } finally {
                is.close();
            }
            logger.debug("Debut de la signature - fin téléchargement, début du MD5");
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < digest.length; i++) {
                sb.append(
                        Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(
                                1));
            }
            logger.debug("Fin de la signature");
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static String generateMD5SignatureFromDOM(String html) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(html.getBytes());
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : digest) {
                sb.append(Integer.toHexString((int) (b & 0xff)));
            }
            //logger.debug("original:" + html);
            //logger.debug("digested:" + digest);
            logger.debug("Signature : " + sb.toString());
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
