package lan.dk.podcastserver.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by kevin on 07/06/15 for HackerRank problem
 */
@Service
public class HtmlService {

    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
    private static final String REFERRER = "http://www.google.fr";
    private static final Integer TIMEOUT = 5000;

    public Connection connectWithDefault(String url) {
        return connect(url)
                .timeout(TIMEOUT)
                .userAgent(USER_AGENT)
                .referrer(REFERRER);
    }

    public Connection connect(String url) {
        return Jsoup.connect(url);
    }

    public Document get(String url) throws IOException {
        return connectWithDefault(url).execute().parse();
    }

}
