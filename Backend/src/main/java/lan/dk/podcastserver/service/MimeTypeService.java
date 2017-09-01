package lan.dk.podcastserver.service;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static io.vavr.API.Try;
import static java.util.Objects.nonNull;
import static lan.dk.podcastserver.manager.worker.updater.YoutubeUpdater.YOUTUBE;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
@Service
public class MimeTypeService {

    private final TikaProbeContentType tikaProbeContentType;
    private final Map<String, String> mimeMap;

    @Autowired
    public MimeTypeService(TikaProbeContentType tikaProbeContentType) {
        this.tikaProbeContentType = tikaProbeContentType;
        mimeMap = HashMap.ofEntries(
                Tuple.of("mp4", "video/mp4"),
                Tuple.of("mp3", "audio/mp3"),
                Tuple.of("flv", "video/flv"),
                Tuple.of("webm", "video/webm"),
                Tuple.of("", "video/mp4")
        );
    }

    public String getMimeType(String extension) {
        if (extension.isEmpty())
            return "application/octet-stream";

        return mimeMap
                .filter(e -> e._1().equals(extension))
                .map(Tuple2::_2)
                .getOrElse(() -> "unknown/" + extension);
    }

    String getExtension(Item item) {
        if (nonNull(item.getMimeType())) {
            return item.getMimeType().replace("audio/", ".").replace("video/", ".");
        }

        if (Objects.equals(YOUTUBE, item.getPodcast().getType()) || StringUtils.containsNone(item.getUrl(), ".")) {
            return ".mp4";
        }

        return "."+FilenameUtils.getExtension(item.getUrl());
    }

    // https://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
    public String probeContentType(Path file) {
        return filesProbeContentType(file)
                .orElse(() -> tikaProbeContentType.probeContentType(file))
                .getOrElse(() -> getMimeType(FilenameUtils.getExtension(file.getFileName().toString())));
    }

    private Option<String> filesProbeContentType(Path file) {
        return Try(() -> Files.probeContentType(file))
                .filter(Objects::nonNull)
                .toOption();
    }

    @RequiredArgsConstructor
    public static class TikaProbeContentType {

        private final Tika tika;

        Option<String> probeContentType(Path file) {
            return Try(() -> tika.detect(file.toFile()))
                .filter(Objects::nonNull)
                .toOption();
        }
    }
}
