package lan.dk.podcastserver.utils;

import javaslang.control.Option;
import javaslang.control.Try;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static MatcherExtractor of(Pattern p, String value) {
        return new MatcherExtractor(p.matcher(value));
    }

    public Option<String> group(Integer number) {
        if (!isFind) {
            return Option.none();
        }

        return Try.of(() -> matcher.group(number)).toOption();
    }

}
