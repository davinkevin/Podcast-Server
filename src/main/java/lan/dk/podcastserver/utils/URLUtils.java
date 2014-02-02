package lan.dk.podcastserver.utils;

import org.apache.commons.io.FilenameUtils;

/**
 * Created by kevin on 01/02/2014.
 */
public class URLUtils {

    public static String getFileNameFromCanalPlusM3U8Url(String m3u8Url) {
        /* http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8 */
        String[] splitUrl = m3u8Url.split(",");

        int lenghtTab = splitUrl.length;
        String urlWithoutAllBandwith = new StringBuffer()
                .append(splitUrl[0])
                .append(splitUrl[lenghtTab-2])
                .append(splitUrl[lenghtTab-1]).toString();

        int posLastSlash = urlWithoutAllBandwith.lastIndexOf("/");

        return FilenameUtils.getName(urlWithoutAllBandwith.substring(0, posLastSlash).replace(".csmil", ""));
    }
}
