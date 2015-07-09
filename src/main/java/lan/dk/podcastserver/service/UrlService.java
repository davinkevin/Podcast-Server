package lan.dk.podcastserver.service;

import lan.dk.podcastserver.utils.URLUtils;
import org.springframework.stereotype.Service;

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

    public URLConnection getStreamWithTimeOut(String stringUrl, Integer timeOutInMilli) throws IOException {
        return URLUtils.getStreamWithTimeOut(stringUrl, timeOutInMilli);
    }

    public URLConnection getStreamWithTimeOut(String stringUrl) throws IOException {
        return URLUtils.getStreamWithTimeOut(stringUrl);
    }

    public static String getFileNameM3U8Url(String url) {
        return URLUtils.getFileNameM3U8Url(url);
    }

    public String urlWithDomain(String urlWithDomain, String domaineLessUrl) {
        return URLUtils.urlWithDomain(urlWithDomain, domaineLessUrl);
    }

}
