package lan.dk.podcastserver.service;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.control.Option;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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
        if (item.getMimeType() != null) {
            return item.getMimeType().replace("audio/", ".").replace("video/", ".");
        }

        if ("Youtube".equals(item.getPodcast().getType()) || item.getUrl().lastIndexOf(".") == -1 ) {
            return ".mp4";
        } else {
            return "."+FilenameUtils.getExtension(item.getUrl());
        }
    }

    // https://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
    public String probeContentType(Path file) {
        return filesProbeContentType(file)
                .orElse(() -> tikaProbeContentType.probeContentType(file))
                .getOrElse(() -> getMimeType(FilenameUtils.getExtension(String.valueOf(file.getFileName()))));
    }

    private Option<String> filesProbeContentType(Path file) {
        return Try.of(() -> Files.probeContentType(file))
                .filter(Objects::nonNull)
                .toOption();
    }

    @RequiredArgsConstructor
    public static class TikaProbeContentType {

        private final Tika tika;

        Option<String> probeContentType(Path file) {
            try {
                return Option.of(tika.detect(file.toFile()));
            } catch (IOException ignored) {
                return Option.none();
            }
        }
    }
}
