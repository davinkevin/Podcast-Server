package lan.dk.podcastserver.manager.worker.sixplay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.davinkevin.podcastserver.service.M3U8Service;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.github.davinkevin.podcastserver.utils.MatcherExtractor;
import com.jayway.jsonpath.TypeRef;
import com.mashape.unirest.http.HttpResponse;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.service.JsonService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;
import static io.vavr.API.*;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Created by kevin on 26/12/2017
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SixPlayExtractor implements Extractor {

    private static final TypeRef<List<M6PlayItem>> TYPE_ITEMS = new TypeRef<List<M6PlayItem>>(){};
    private static final String ITEMS_EXTRACTOR = "clips[*]";
    private static final String INFO_URL = "https://pc.middleware.6play.fr/6play/v2/platforms/m6group_web/services/6play/videos/%s_%s?with=clips&csa=5";
    private static final PatternExtractor URL_EXTRACTOR = from(Pattern.compile("^.+6play\\.fr/.+-([a-z])_([0-9]*)"));

    private final JsonService jsonService;
    private final M3U8Service m3U8Service;
    private final UrlService urlService;

    @Override
    public DownloadingItem extract(Item item) {

        MatcherExtractor m = URL_EXTRACTOR.on(item.getUrl());

        List<String> urls = Option.sequence(List(m.group(1), m.group(2)))
                .map(s -> Tuple(s.get(0), s.get(1)))
                .map(t -> t.map1(SixPlayExtractor::expandType))
                .map(SixPlayExtractor::toJsonUrl)
                .flatMap(jsonService::parseUrl)
                .map(JsonService.to(ITEMS_EXTRACTOR, TYPE_ITEMS))
                .getOrElse(List::empty)
                .flatMap(this::keepBestQuality)
                .map(M6PlayAssets::getFull_physical_path);


        return DownloadingItem.builder()
                .item(item)
                .urls(urls)
                .filename(getFileName(item))
                .build();
    }

    private static String expandType(String type) {
        return Match(type).of(
            Case($("p"), "playlist"),
            Case($("c"), "clip")
        );
    }

    private static String toJsonUrl(Tuple2<String, String> params) {
        return String.format(INFO_URL, params._1(), params._2());
    }

    private Option<M6PlayAssets> keepBestQuality(M6PlayItem item) {
        List<M6PlayAssets> assets = item.getAssets();

        return assets
                .filter(i -> !i.getProtocol().equalsIgnoreCase("primetime"))
                .find(i -> StringUtils.contains(i.getVideo_quality(), "sd3"))
                .flatMap(this::transformSd3Url)
                .orElse(() -> assets.find(i -> "usp_hls_h264".equalsIgnoreCase(i.getType())))
                .orElse(() -> assets.find(i -> "hq".equalsIgnoreCase(i.getVideo_quality())))
                .orElse(() -> assets.find(i -> "hd".equalsIgnoreCase(i.getVideo_quality())))
                .orElse(() -> assets.find(i -> "sd".equalsIgnoreCase(i.getVideo_quality())));
    }

    private Option<M6PlayAssets> transformSd3Url(M6PlayAssets asset) {
        String realURL = urlService.getRealURL(asset.getFull_physical_path());

        return Try(() -> urlService.get(realURL)
                .header(UrlService.USER_AGENT_KEY, UrlService.USER_AGENT_MOBILE)
                .asString())
                .map(HttpResponse::getRawBody)
                .flatMap(is -> m3U8Service.findBestQuality(is).toTry())
                .map(url -> urlService.addDomainIfRelative(realURL, url))
                .map(url -> new M6PlayAssets(asset.getVideo_quality(), url, asset.getType(), asset.getProtocol()))
                .toOption();
    }

    @Override
    public String getFileName(Item item) {
        return Extractor.super.getFileName(item) + ".mp4";
    }

    @Override
    public Integer compatibility(String url) {
        return SixPlayUpdater.isFrom6Play(url);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class M6PlayItem {
        @Getter @Setter private List<M6PlayAssets> assets;
    }

    @AllArgsConstructor @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class M6PlayAssets {
        @Setter @Getter private String video_quality;
        @Setter @Getter private String full_physical_path;
        @Setter @Getter private String type;
        @Setter @Getter private String protocol;
    }
}
