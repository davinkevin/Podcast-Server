package lan.dk.podcastserver.service;

import lan.dk.podcastserver.utils.URLUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLConnection;

/**
 * Created by kevin on 09/07/15 for Podcast Server
 */
@Service
public class UrlService {

    public Reader getReaderFromURL (String url) throws IOException {
        return URLUtils.getReaderFromURL(url);
    }

    public String getM3U8UrlFormMultiStreamFile(String url) {
        return URLUtils.getM3U8UrlFormMultiStreamFile(url);
    }

    public String getRealURL(String url) {
        return URLUtils.getRealURL(url);
    }

    public URLConnection getConnectionWithTimeOut(String stringUrl, Integer timeOutInMilli) throws IOException {
        return URLUtils.getConnectionWithTimeOut(stringUrl, timeOutInMilli);
    }

    public URLConnection getConnection(String stringUrl) throws IOException {
        return URLUtils.getConnection(stringUrl);
    }

    public String getFileNameM3U8Url(String url) {
        return URLUtils.getFileNameM3U8Url(url);
    }

    public String urlWithDomain(String urlWithDomain, String domaineLessUrl) {
        return URLUtils.urlWithDomain(urlWithDomain, domaineLessUrl);
    }

    @PostConstruct
    public void postConstruct() {
        System.setProperty("http.agent", HtmlService.USER_AGENT);
    }

    public BufferedReader urlAsReader(String url) throws IOException {
        return URLUtils.urlAsReader(url);
    }
}
