package lan.dk.podcastserver.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

/**
 * Created by kevin on 07/06/15 for HackerRank problem
 */
@Service
public class HtmlService {

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36";
    private static final String REFERRER = "http://www.google.fr";
    private static final Integer TIMEOUT = 5000;

    public Connection connectWithDefault(String url) {
        return Jsoup.connect(url)
                .timeout(TIMEOUT)
                .userAgent(USER_AGENT)
                .referrer(REFERRER);
    }

    public Connection connect(String url) {
        return Jsoup.connect(url);
    }

}
