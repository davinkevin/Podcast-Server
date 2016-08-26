package lan.dk.podcastserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;

import static java.util.stream.Collectors.joining;

/**
 * Created by kevin on 24/01/15 for Podcast Server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    final UrlService urlService;

    public String generateSignatureFromURL(String urlAsString) {
        try(BufferedReader in = urlService.asReader(urlAsString)) {
            return generateMD5Signature(in.lines().collect(joining()));
        } catch (IOException e) {
            log.error("Error during signature of podcast at url {}", urlAsString, e);
        }
        return StringUtils.EMPTY;
    }
    
    public String generateMD5Signature(String html){
        return DigestUtils.md5Hex(html);
    }
}
