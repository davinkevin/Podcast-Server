package lan.dk.podcastserver.Utils;

import lan.dk.podcastserver.utils.URLUtils;
import org.junit.Test;

/**
 * Created by kevin on 01/02/2014.
 */
public class URLUtilsTest {

    @Test
    public void test() {
        System.out.print(URLUtils.getFileNameFromCanalPlusM3U8Url("http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8"));
    }
}
