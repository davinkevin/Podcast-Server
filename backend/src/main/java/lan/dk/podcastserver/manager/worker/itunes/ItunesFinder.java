package lan.dk.podcastserver.manager.worker.itunes;

import com.github.davinkevin.podcastserver.utils.MatcherExtractor;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lan.dk.podcastserver.manager.worker.rss.RSSFinder;
import lan.dk.podcastserver.service.JsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;

/**
 * Created by kevin on 12/05/2018
 */
@Service
@RequiredArgsConstructor
public class ItunesFinder implements Finder {
    private static final String ITUNES_API = "https://itunes.apple.com/lookup?id=";
    private static final MatcherExtractor.PatternExtractor ARTIST_ID = from(Pattern.compile(".*id=?([\\d]+).*"));

    private final RSSFinder rssFinder;
    private final JsonService jsonService;

    @Override
    public Podcast find(String url) {
        return ARTIST_ID.on(url)
                .group(1)
                .map(id -> ITUNES_API + id)
                .flatMap(jsonService::parseUrl)
                .map(JsonService.to("results[0].feedUrl", String.class))
                .map(rssFinder::find)
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("itunes.apple.com") ? 1 : Integer.MAX_VALUE;
    }
}
