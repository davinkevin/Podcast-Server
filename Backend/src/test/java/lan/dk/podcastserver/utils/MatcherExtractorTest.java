package lan.dk.podcastserver.utils;

import javaslang.control.Option;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 04/03/2017
 */
public class MatcherExtractorTest {

    @Test
    public void should_return_value() {
        /* GIVEN */
        String s = "foo";
        Pattern p = Pattern.compile("(.*)");

        /* WHEN  */
        Option<String> v = MatcherExtractor.of(p, s).group(1);

        /* THEN  */
        assertThat(v).containsExactly("foo");
    }

    @Test
    public void should_return_no_value() {
        /* GIVEN */
        String s = "";
        Pattern p = Pattern.compile("abc");

        /* WHEN  */
        Option<String> v = MatcherExtractor.of(p, s).group(1);

        /* THEN  */
        assertThat(v).isEqualTo(Option.none());
    }

}