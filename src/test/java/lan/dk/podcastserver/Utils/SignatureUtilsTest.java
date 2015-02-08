package lan.dk.podcastserver.Utils;

import lan.dk.podcastserver.utils.SignatureUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignatureUtilsTest {

    private final Logger logger = LoggerFactory.getLogger(SignatureUtilsTest.class);
    
    Integer numberOfRead = 1000;
    String url = "http://www.season1.fr/category/Podcast/feed/";
    
    @Test
    public void should_generate_1_000_Digest () {
        for (int i = numberOfRead; i > 0; i--) {
            logger.info("Digest n°{}", i);
            SignatureUtils.generateSignatureFromURL(url);
        }
    }
    
}
