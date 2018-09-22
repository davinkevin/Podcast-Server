package lan.dk.podcastserver.manager.worker.gulli;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.utils.MatcherExtractor;
import com.jayway.jsonpath.TypeRef;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;
import static io.vavr.API.List;

/**
 * Created by kevin on 03/12/2017
 */
@Slf4j
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class GulliExtractor implements Extractor {

    private static final MatcherExtractor.PatternExtractor NUMBER_IN_PLAYLIST_EXTRACTOR = from(Pattern.compile("playlistItem\\(([^)]*)\\);"));
    private static final MatcherExtractor.PatternExtractor PLAYLIST_EXTRACTOR = from(Pattern.compile("playlist:\\s*(.*?(?=events:))", Pattern.DOTALL));
    private static final TypeRef<List<GulliExtractor.GulliItem>> GULLI_ITEM_TYPE_REF = new TypeRef<List<GulliItem>>(){};

    private final HtmlService htmlService;
    private final JsonService jsonService;

    @Override
    public DownloadingItem extract(Item item) {
        return this._getItemUrl(item)
                .map(url -> DownloadingItem.builder()
                        .urls(List(url))
                        .item(item)
                        .filename(this.getFileName(item))
                        .build()
                )
                .getOrElseThrow(() -> new RuntimeException("Gulli Url extraction failed"));
    }

    private Option<String> _getItemUrl(Item item) {
        return htmlService.get(item.getUrl())
                .map(d -> d.select("script"))
                .flatMap(scripts -> List.ofAll(scripts).find(e -> e.html().contains("playlist:")))
                .flatMap(this::getPlaylistFromGulliScript);
    }

    private Option<String> getPlaylistFromGulliScript(Element element) {
        Option<String> playlist = PLAYLIST_EXTRACTOR.on(element.html()).group(1);
        return NUMBER_IN_PLAYLIST_EXTRACTOR
                .on(element.html()).group(1)
                .map(Integer::valueOf)
                .flatMap(v -> playlist.map(s -> Tuple.of(v, s)))
                .map(t -> t.map2(jsonService::parse))
                .map(t -> t.map2(JsonService.to(GULLI_ITEM_TYPE_REF)))
                .map(t -> t.apply((v, s) -> s.get(v)))
                .flatMap(i -> i.getSources().find(s -> s.file.contains("mp4")))
                .map(GulliItem.GulliSource::getFile);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GulliItem {
        @Getter
        @Setter
        private List<GulliExtractor.GulliItem.GulliSource> sources;

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class GulliSource {
            @Getter @Setter private String file;
        }

    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("replay.gulli.fr") ? 1 : Integer.MAX_VALUE;
    }
}
