package lan.dk.podcastserver.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by kevin on 07/06/15 for HackerRank problem
 */
@Slf4j
@Service
public class HtmlService {

    static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
    private static final String REFERRER = "http://www.google.fr";
    private static final Integer TIMEOUT = 5000;

    Connection connectWithDefault(String url) {
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

    public Document parse(String html) {
        return Jsoup.parse(html);
    }

    public static Collector<Element, Elements, Elements> toElements() {
        return new ElementsCollector();
    }
    private static class ElementsCollector implements Collector<Element, Elements, Elements> {
        @Override public Supplier<Elements> supplier() { return Elements::new; }
        @Override public BiConsumer<Elements, Element> accumulator() { return Elements::add; }
        @Override public BinaryOperator<Elements> combiner() { return (left, right) -> { left.addAll(right); return left; }; }
        @Override public Function<Elements, Elements> finisher() { return Function.identity(); }
        @Override public Set<Characteristics> characteristics() { return EnumSet.of(Characteristics.UNORDERED); }
    }
}
