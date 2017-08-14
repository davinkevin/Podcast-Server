package lan.dk.podcastserver.service;

import com.mashape.unirest.http.HttpResponse;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collector;
import static io.vavr.API.*;
/**
 * Created by kevin on 07/06/15 for HackerRank problem
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HtmlService {

    private final UrlService urlService;

    public Option<Document> get(String url) {
        return Try(() -> urlService.get(url)
                    .header(UrlService.USER_AGENT_KEY, UrlService.USER_AGENT_DESKTOP)
                .asBinary())
                    .filter(h -> h.getStatus() < 400)
                    .map(HttpResponse::getBody)
                    .mapTry(is -> Jsoup.parse(is, StandardCharsets.UTF_8.name(), ""))
                    .onFailure(e -> log.error("Error during HTML Fetching of {}", url, e))
                .toOption();
    }

    public Document parse(String html) {
        return Jsoup.parse(html);
    }

    public static Collector<Element, Elements, Elements> toElements() {
        return Collector.of(
                Elements::new,
                Elements::add,
                (left, right) -> { left.addAll(right); return left; },
                Collector.Characteristics.UNORDERED
        );
    }
}
