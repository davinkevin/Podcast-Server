package lan.dk.podcastserver.utils;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(SignatureUtils.class);
    public static final Integer MAX_NUMBER_OF_REDIRECTION = 10;
    
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

    public static Reader getReaderFromURL (String url) throws IOException {
        URL urlObject = new URL(url);
        return new BufferedReader(new InputStreamReader(urlObject.openStream(), "UTF-8"));
    }

    public static String getM3U8UrlFormMultiStreamFile(String url) {
        String urlToReturn = null;

        if (url == null)
            return null;

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (!inputLine.startsWith("#")) {
                    urlToReturn = inputLine;
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlToReturn;
    }

    public static String getRealURL(String url) {
        return getRealURL(url, 0);
    }

    private static String getRealURL(String url, Integer numberOfRedirection) {
        if (MAX_NUMBER_OF_REDIRECTION < numberOfRedirection) {
            throw new RuntimeException("Too Many Redirections");
        }

        try {
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            if (isARedirection(conn.getResponseCode())) {
                conn.disconnect();
                return getRealURL(conn.getHeaderField("Location"), numberOfRedirection+1);
            }
        } catch (IOException e) {
            logger.error("Error during retrieval of the real URL for {} at {} redirection", url, numberOfRedirection);
        }

        return url;
    }
    private static Boolean isARedirection(int status) {
        return status != HttpURLConnection.HTTP_OK && (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER);
    }
}
