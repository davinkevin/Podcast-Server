package lan.dk.podcastserver.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * Created by kevin on 21/07/2016.
 */
@Service
public class M3U8Service {

    public Optional<String> findBestQuality(InputStream is) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            return buffer
                    .lines()
                    .filter(l -> l.contains("audio") && l.contains("video"))
                    .reduce((u, v) -> v);
        } catch (IOException | RuntimeException ignored) {
            /* RuntimeException added because buffer.lines only throw UncheckedIOException */
            return Optional.empty();
        }
    }
}
