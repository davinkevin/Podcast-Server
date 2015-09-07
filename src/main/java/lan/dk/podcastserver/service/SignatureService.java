package lan.dk.podcastserver.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by kevin on 24/01/15 for Podcast Server
 */
@Slf4j
@Service
public class SignatureService {

    final UrlService urlService;

    @Autowired
    public SignatureService(UrlService urlService) {
        this.urlService = urlService;
    }

    public String generateSignatureFromURL(String urlAsString) {
        try {
            return DigestUtils.md5Hex(urlService.getConnection(urlAsString).getInputStream());
        } catch (IOException e) {
            log.error("Error during signature of podcast at url {}", urlAsString, e);
        }
        return StringUtils.EMPTY;
    }
    
    public String generateMD5Signature(String html){
        return DigestUtils.md5Hex(html);
    }
}
