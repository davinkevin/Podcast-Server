package lan.dk.podcastserver.utils;

import lan.dk.podcastserver.service.SignatureService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * Created by kevin on 01/02/2014.
 */
public class URLUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(SignatureService.class);
    private static final Integer DEFAULT_TIME_OUT_IN_MILLI = 10000;
    private static final Integer MAX_NUMBER_OF_REDIRECTION = 10;
    private static final String PROTOCOL_SEPARATOR = "://";

    private static String getFileNameFromCanalPlusM3U8Url(String m3u8Url) {
        /* http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8 */
        String[] splitUrl = m3u8Url.split(",");

        int lenghtTab = splitUrl.length;
        String urlWithoutAllBandwith = splitUrl[0] + splitUrl[lenghtTab - 2] + splitUrl[lenghtTab - 1];

        int posLastSlash = urlWithoutAllBandwith.lastIndexOf("/");

        return FilenameUtils.getName(urlWithoutAllBandwith.substring(0, posLastSlash).replace(".csmil", ""));
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

        if (Objects.nonNull(urlToReturn)) {
            urlToReturn = StringUtils.substringBeforeLast(urlToReturn, "?");
        }

        return urlToReturn;
    }

    public static String getRealURL(String url) {
        return getRealURL(url, 0);
    }

    private static String getRealURL(String url, Integer numberOfRedirection) {
        if (MAX_NUMBER_OF_REDIRECTION <= numberOfRedirection) {
            throw new RuntimeException("Too Many Redirections");
        }

        try {
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setInstanceFollowRedirects(false);
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
    
    public static URLConnection getConnectionWithTimeOut(String stringUrl, Integer timeOutInMilli) throws IOException {
        URL url = new URL(stringUrl);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setReadTimeout(timeOutInMilli);
        urlConnection.setConnectTimeout(timeOutInMilli);
        return urlConnection;
    }
    
    public static URLConnection getConnection(String stringUrl) throws IOException {
        return getConnectionWithTimeOut(stringUrl, DEFAULT_TIME_OUT_IN_MILLI);
    }

    public static String getFileNameM3U8Url(String url) {
        if (StringUtils.contains(url, "canal-plus") || StringUtils.contains(url, "cplus"))
            return getFileNameFromCanalPlusM3U8Url(url);
        
        /* In other case, remove the url parameter and extract the filename */
        return FilenameUtils.getBaseName(StringUtils.substringBeforeLast(url, "?")).concat(".mp4");
    }

    public static String urlWithDomain(String urlWithDomain, String domaineLessUrl) {
        if (domaineLessUrl.contains("://"))
            return domaineLessUrl;

        return StringUtils.substringBeforeLast(urlWithDomain, "/") + "/" + domaineLessUrl;
    }
}
