package lan.dk.podcastserver.manager.worker.mycanal;

import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javax.validation.constraints.NotEmpty;

import static io.vavr.API.None;
import static io.vavr.API.Option;

/**
 * Created by kevin on 26/12/2017
 */
@Slf4j
public class MyCanalUtils {

    static Option<String> extractJsonConfig(String text) {
        String startToken = "__data=";
        String endToken = "};";

        if (!text.contains(startToken) || !text.contains(endToken)) {
            log.error("Structure of MyCanal page changed");
            return None();
        }

        int begin = text.indexOf(startToken);
        int end = text.indexOf(endToken, begin);
        return Option(text.substring(begin + startToken.length(), end + 1));
    }

    static int compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "www.mycanal.fr") ? 1 : Integer.MAX_VALUE;
    }

}
