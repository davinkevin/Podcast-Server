package lan.dk.podcastserver.utils;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.None;
import static io.vavr.API.Try;

/**
 * Created by kevin on 04/03/2017
 */
public class MatcherExtractor {

    private final Matcher matcher;
    private final Boolean isFind;

    private MatcherExtractor(Matcher matcher) {
        this.matcher = matcher;
        this.isFind = matcher.find();
    }

    public static PatternExtractor from(Pattern pattern) {
        return new PatternExtractor(pattern);
    }

    public Option<String> group(Integer group) {
        if (!isFind) {
            return None();
        }

        return Try(() -> matcher.group(group)).toOption();
    }

    public Option<List<String>> groups() {
        if (!isFind) {
            return None();
        }

        return Stream.from(1)
                .take(matcher.groupCount())
                .map(matcher::group)
                .toList()
                .transform(Option::of);
    }

    public static class PatternExtractor {

        private final Pattern pattern;

        private PatternExtractor(Pattern pattern) { this.pattern = pattern; }

        public MatcherExtractor on(String value) { return new MatcherExtractor(pattern.matcher(value)); }

    }

}
