package lan.dk.podcastserver.Utils;

import lan.dk.podcastserver.utils.DigestUtils;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * Created by kevin on 26/12/2013.
 */
public class DigestTest {

    @Test
    public void DigestHTML() {
        Assert.notNull(DigestUtils.generateMD5SignatureFromDOM("uneNouvellePhrase"));
    }
}
