package lan.dk.podcastserver.manager.worker.youtube;

import com.github.davinkevin.podcastserver.service.HtmlService;
import io.vavr.collection.Stream;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.util.function.Function;

import static io.vavr.API.Option;

/**
 * Created by kevin on 22/02/15
 */
@Slf4j
@Service("YoutubeFinder")
@RequiredArgsConstructor
public class YoutubeFinder implements Finder {

    private final HtmlService htmlService;

    @Override
    public Podcast find(String url) {
        return htmlService
                .get(url)
                .map(p -> podcastFromHtml(url, p))
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast podcastFromHtml(String url, Document p) {
        return Podcast.builder()
                .url(url)
                .type("Youtube")
                .title(getMeta(p).apply("title"))
                .description(getMeta(p).apply("description"))
                .cover(getCover(p))
                .build();
    }

    private Function<String, String> getMeta(Document page) {
        return metaName -> Option(page.select("meta[name="+ metaName +"]").first())
                .map(e -> e.attr("content"))
                .getOrElse("");
    }

    private Cover getCover(Document page) {
        return Option(page.select("img.channel-header-profile-image").first())
                .map(e -> e.attr("src"))
                .map(e -> Cover
                        .builder()
                        .url(e)
                        .height(200)
                        .width(200)
                        .build()
                )
                .getOrElse(Cover.DEFAULT_COVER);
    }


    public Integer compatibility(@NotEmpty String url) {
        return isYoutubeUrl(url) ? 1 : Integer.MAX_VALUE;
    }

    private Boolean isYoutubeUrl(String url) {
        return Stream.of("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                .exists(url::contains);
    }
}
