package lan.dk.podcastserver.service;

import com.google.common.collect.Maps;
import lan.dk.podcastserver.entity.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
@Service
public class MimeTypeService {

    private final Map<String, String> mimeMap;
    private final TikaProbeContentType tikaProbeContentType;

    @Autowired
    public MimeTypeService(TikaProbeContentType tikaProbeContentType) {
        this.tikaProbeContentType = tikaProbeContentType;
        mimeMap = Maps.newHashMap();
        mimeMap.put("mp4", "video/mp4");
        mimeMap.put("mp3", "audio/mp3");
        mimeMap.put("flv", "video/flv");
        mimeMap.put("webm", "video/webm");
        mimeMap.put("", "video/mp4");
    }

    public String getMimeType(String extension) {
        if (extension.isEmpty())
            return "application/octet-stream";

        if (mimeMap.containsKey(extension)) {
            return mimeMap.get(extension);
        } else {
            return "unknown/" + extension;
        }
    }

    public String getExtension(Item item) {
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
                .orElseGet(() -> tikaProbeContentType.probeContentType(file)
                .orElseGet(() -> getMimeType(FilenameUtils.getExtension(String.valueOf(file.getFileName())))));
    }

    private Optional<String> filesProbeContentType(Path file) {
        String mimeType = null;

        try { mimeType = Files.probeContentType(file); } catch (IOException ignored) {}

        return Optional.ofNullable(mimeType);
    }

    @RequiredArgsConstructor
    public static class TikaProbeContentType {

        private final Tika tika;

        Optional<String> probeContentType(Path file) {
            try {
                return Optional.of(tika.detect(file.toFile()));
            } catch (IOException ignored) {
                return Optional.empty();
            }
        }
    }
}
