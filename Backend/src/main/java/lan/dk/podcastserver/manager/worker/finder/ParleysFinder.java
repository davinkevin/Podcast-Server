package lan.dk.podcastserver.manager.worker.finder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 12/06/2016 for PodcastServer
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ParleysFinder implements Finder {

    private static final Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/channel/([^/]*)");
    private static final String API_URL = "https://api.parleys.com/api/channel.json/%s";

    private final UrlService urlService;
    private final JsonService jsonService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return extractIdOf(url)
                .map(id -> String.format(API_URL, id))
                .flatMap(urlService::newURL)
                .flatMap(jsonService::parse)
                .map(d -> d.read("$", ParleysPodcast.class))
                .map(p -> Podcast
                            .builder()
                                .title(p.getName())
                                .description(p.getDescription())
                                .cover(imageService.getCoverFromURL(p.coverUrl()))
                                .type("Parleys")
                                .url(url)
                            .build())
                .orElse(Podcast.DEFAULT_PODCAST);
    }

    private Optional<String> extractIdOf(String url) {
        Matcher m = ID_PARLEYS_PATTERN.matcher(url);

        if (!m.find()) return Optional.empty();

        return Optional.of(m.group(1));
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "parleys.com") ? 1 : Integer.MAX_VALUE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParleysPodcast {
        @Getter @Setter private String name;
        @Getter @Setter private String description;
        @Setter private String thumbnail;
        @Setter private String basePath;

        String coverUrl() {
            return this.basePath + thumbnail;
        }
    }
}
