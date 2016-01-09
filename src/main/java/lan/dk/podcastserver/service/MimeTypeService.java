package lan.dk.podcastserver.service;

import com.google.common.collect.Maps;
import lan.dk.podcastserver.entity.Item;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
@Service
public class MimeTypeService {

    Tika tika = new Tika();
    Map<String, String> MimeMap;

    public MimeTypeService() {
        MimeMap = Maps.newHashMap();
        MimeMap.put("mp4", "video/mp4");
        MimeMap.put("mp3", "audio/mp3");
        MimeMap.put("flv", "video/flv");
        MimeMap.put("webm", "video/webm");
        MimeMap.put("", "video/mp4");
    }

    public String getMimeType(String extension) {
        if (extension.isEmpty())
            return "application/octet-stream";

        if (MimeMap.containsKey(extension)) {
            return MimeMap.get(extension);
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
    public String probeContentType(Path file) throws IOException {
        String mimetype = Files.probeContentType(file);
        if (mimetype != null)
            return mimetype;

        mimetype = tika.detect(file.toFile());
        if (mimetype != null)
            return mimetype;

        return getMimeType(FilenameUtils.getExtension(String.valueOf(file.getFileName())));
    }
}
