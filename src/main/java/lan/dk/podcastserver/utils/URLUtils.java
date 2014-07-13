package lan.dk.podcastserver.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public static boolean isAValidURL(String url) {
        URL u = null;
        HttpURLConnection huc = null;
        try {
            u = new URL(url);
            huc =  (HttpURLConnection)  u.openConnection();
            huc.setRequestMethod("HEAD");
            huc.connect();
            return (huc.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (huc != null)
                huc.disconnect();
        }
        return false;
    }

    public static Reader getReaderFromURL (String url) {
        try {
            URL urlObject = new URL(url);
            return new BufferedReader(new InputStreamReader(urlObject.openStream(), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
