package lan.dk.podcastserver.utils;

import javaslang.control.Option;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 04/03/2017
 */
public class MatcherExtractorTest {

    @Test
    public void should_return_value() {
        /* GIVEN */
        String s = "foo";
        String p = "(.*)";

        /* WHEN  */
        Option<String> v = MatcherExtractor.from(p).extractFrom(s).group(1);

        /* THEN  */
        assertThat(v).containsExactly("foo");
    }

    @Test
    public void should_return_no_value() {
        /* GIVEN */
        String s = "";
        String p = "abc";

        /* WHEN  */
        Option<String> v = MatcherExtractor.from(p).extractFrom(s).group(1);

        /* THEN  */
        assertThat(v).isEqualTo(Option.none());
    }

}