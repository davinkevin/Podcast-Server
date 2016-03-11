package lan.dk.podcastserver.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Created by kevin on 09/07/15 for Podcast Server
 */
@Slf4j
@Service
public class UrlService {

    private static final Integer DEFAULT_TIME_OUT_IN_MILLI = 10000;
    private static final Integer MAX_NUMBER_OF_REDIRECTION = 10;
    private static final String PROTOCOL_SEPARATOR = "://";

    public Optional<URL> newURL(String url) {
        try {
            return Optional.of(new URL(url));
        } catch (MalformedURLException e) {
            log.error("Error during creation of URL {}", url, e);
            return Optional.empty();
        }
    }

    public String getM3U8UrlFormMultiStreamFile(String url) {
        if (isNull(url))
            return null;

        try(BufferedReader in = urlAsReader(url)) {
            return in
                    .lines()
                    .filter(l -> !l.startsWith("#"))
                    .reduce((acc, val) -> val)
                    .map(s -> StringUtils.substringBeforeLast(s, "?"))
                    .map(s -> (isRelativeUrl(s)) ? urlWithDomain(url, s) : s)
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getRealURL(String url) {
        return getRealURL(url, 0);
    }

    public URLConnection getConnectionWithTimeOut(String stringUrl, Integer timeOutInMilli) throws IOException {
        URL url = new URL(stringUrl);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setReadTimeout(timeOutInMilli);
        urlConnection.setConnectTimeout(timeOutInMilli);
        return urlConnection;
    }

    public URLConnection getConnection(String stringUrl) throws IOException {
        return getConnectionWithTimeOut(stringUrl, DEFAULT_TIME_OUT_IN_MILLI);
    }

    public String getFileNameM3U8Url(String url) {
        if (StringUtils.contains(url, "canal-plus") || StringUtils.contains(url, "cplus"))
            return getFileNameFromCanalPlusM3U8Url(url);

        /* In other case, remove the url parameter and extract the filename */
        return FilenameUtils.getBaseName(StringUtils.substringBeforeLast(url, "?")).concat(".mp4");
    }

    public String urlWithDomain(String urlWithDomain, String domaineLessUrl) {
        if (domaineLessUrl.contains(PROTOCOL_SEPARATOR))
            return domaineLessUrl;

        return StringUtils.substringBeforeLast(urlWithDomain, "/") + "/" + domaineLessUrl;
    }

    public BufferedReader urlAsReader(String url) throws IOException {
        return urlAsReader(new URL(url));
    }

    public BufferedReader urlAsReader(URL url) throws IOException {
        return new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
    }

    private static String getFileNameFromCanalPlusM3U8Url(String m3u8Url) {
        /* http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8 */
        String[] splitUrl = m3u8Url.split(",");

        int lenghtTab = splitUrl.length;
        String urlWithoutAllBandwith = splitUrl[0] + splitUrl[lenghtTab - 2] + splitUrl[lenghtTab - 1];

        int posLastSlash = urlWithoutAllBandwith.lastIndexOf("/");

        return FilenameUtils.getName(urlWithoutAllBandwith.substring(0, posLastSlash).replace(".csmil", ""));
    }

    private static boolean isRelativeUrl(String urlToReturn) {
        return !urlToReturn.contains("://");
    }

    private String getRealURL(String url, Integer numberOfRedirection) {
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
            log.error("Error during retrieval of the real URL for {} at {} redirection", url, numberOfRedirection);
        }

        return url;
    }

    private Boolean isARedirection(int status) {
        return status != HttpURLConnection.HTTP_OK && (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER);
    }

    public Optional<String> getPageFromURL(String url) {
        try (InputStream in = new URL(url).openStream()) {
            return Optional.of(IOUtils.toString(in));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @PostConstruct
    public void postConstruct() {
        System.setProperty("http.agent", HtmlService.USER_AGENT);
    }
}
