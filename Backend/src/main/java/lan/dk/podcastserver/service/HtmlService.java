package lan.dk.podcastserver.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by kevin on 07/06/15 for HackerRank problem
 */
@Slf4j
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

    private Connection connect(String url) {
        return Jsoup.connect(url);
    }

    public Optional<Document> get(String url) {
        try {
            return Optional.of(connectWithDefault(url).execute().parse());
        } catch (IOException e) {
            log.error("Error during HTML Fetching of {}", url, e);
            return Optional.empty();
        }
    }

}
