package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.HtmlService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 22/02/15.
 */
@Slf4j
@Service("YoutubeFinder")
public class YoutubeFinder implements Finder {

    private HtmlService htmlService;

    @Autowired
    public YoutubeFinder(HtmlService htmlService) {
        this.htmlService = htmlService;
    }

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return htmlService
                .get(url)
                .map(p -> podcastFromHtml(url, p))
                .orElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast podcastFromHtml(String url, Document p) {
        return Podcast
                .builder()
                    .url(url)
                    .type("Youtube")
                    .title(getTitle(p))
                    .description(getDescription(p))
                    .cover(getCover(p))
                .build();
    }

    private String getDescription(Document page) {
        Element elementWithExternalId = page.select("meta[name=description]").first();
        if (elementWithExternalId != null) {
            return elementWithExternalId.attr("content");
        }

        return "";
    }

    private String getTitle(Document page) {
        Element elementWithExternalId = page.select("meta[name=title]").first();
        if (elementWithExternalId != null) {
            return elementWithExternalId.attr("content");
        }

        return "";
    }

    private Cover getCover(Document page) {
        Element elementWithExternalId = page.select("img.channel-header-profile-image").first();
        if (nonNull(elementWithExternalId)) {
            return Cover
                    .builder()
                        .url(elementWithExternalId.attr("src"))
                        .height(200)
                        .width(200)
                    .build();
        }

        return new Cover();
    }


    public Integer compatibility(@NotEmpty String url) {
        return isYoutubeUrl(url) ? 1 : Integer.MAX_VALUE;
    }

    private Boolean isYoutubeUrl(String url) {
        return Stream.of("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/").anyMatch(url::contains);
    }
}
