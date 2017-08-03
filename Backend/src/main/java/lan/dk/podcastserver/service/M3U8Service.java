package lan.dk.podcastserver.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.util.Objects.isNull;

/**
 * Created by kevin on 21/07/2016.
 */
@Service
@RequiredArgsConstructor
public class M3U8Service {

    private final UrlService urlService;

    public Option<String> findBestQuality(InputStream is) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            return Option.ofOptional(buffer
                    .lines()
                    .filter(l -> l.contains("audio") && l.contains("video"))
                    .reduce((u, v) -> v));
        } catch (IOException | RuntimeException ignored) {
            /* RuntimeException added because buffer.lines only throw UncheckedIOException */
            return Option.none();
        }
    }

    public String getM3U8UrlFormMultiStreamFile(String url) {
        if (isNull(url))
            return null;

        try(BufferedReader in = urlService.asReader(url)) {
            return in
                    .lines()
                    .filter(l -> !l.startsWith("#"))
                    .reduce((acc, val) -> val)
                    .map(s -> StringUtils.substringBeforeLast(s, "?"))
                    .map(s -> urlService.addDomainIfRelative(url, s))
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
