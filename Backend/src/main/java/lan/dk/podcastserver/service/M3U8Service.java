package lan.dk.podcastserver.service;

import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.h2.expression.Function;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;

import static io.vavr.API.*;
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
            List<String> lines = buffer.lines().collect(List.collector());

            return _findBestQuality(lines);
        } catch (IOException | RuntimeException ignored) {
            /* RuntimeException added because buffer.lines only throw UncheckedIOException */
            return None();
        }
    }

    private  Option<String> _findBestQuality(List<String> lines) {
        return lines.zipWith(lines.tail(), API::Tuple)
                .filter(t -> t._1().startsWith("#EXT-X-STREAM-INF:"))
                .map(t -> t.map1(p -> p.replace("#EXT-X-STREAM-INF:", "")))
                .map(this::constructParameters)
                .sorted(Comparator.comparing(M3U8Parameters::getBandwidth).reversed())
                .map(M3U8Parameters::getUrl)
                .headOption();
    }

    private M3U8Parameters constructParameters(Tuple2<String, String> params) {

        M3U8Parameters.M3U8ParametersBuilder builder = M3U8Parameters.builder();

        Set(params._1().replace(",avc1", "-avc1").split(","))
            .filter(s -> s.length() > 1)
            .map(p -> p.split("="))
            .map(kv -> Tuple(kv[0], kv[1]))
            .forEach(t -> Match(t._1()).of(
                    Case($("BANDWIDTH"), o -> API.run(() -> builder.bandwidth(Integer.parseInt(t._2())))),
                    Case($(), o -> API.run(() -> {}))
                    /*
                    Case($("PROGRAM-ID"), o -> API.run(() -> builder.programId(t._2()))),
                    Case($("CODECS"), o -> API.run(() -> builder.codecs(t._2()))),
                    Case($("RESOLUTION"), o -> API.run(() -> builder.resolution(t._2()))),
                    */
            ));

        return builder.url(params._2()).build();
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

    @Builder
    private static class M3U8Parameters {
        @Getter private final String url;
        @Getter private final Integer bandwidth;

        /*
        private final String programId;
        private final String codecs;
        private final String resolution;
        */


    }

}
