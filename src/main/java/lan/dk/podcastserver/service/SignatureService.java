package lan.dk.podcastserver.service;

import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by kevin on 24/01/15.
 */
@Service
public class SignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SignatureService.class);

    public String generateSignatureFromURL(String urlAsString) {
        try {
            return DigestUtils.md5Hex(URLUtils.getConnection(urlAsString).getInputStream());
        } catch (IOException e) {
            logger.error("Error during signature of podcast at url {}", urlAsString, e);
        }
        return "";
    }
    
    public String generateMD5Signature(String html){
        return DigestUtils.md5Hex(html);
    }
}
