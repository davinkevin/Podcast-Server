package lan.dk.podcastserver.manager.worker.tf1replay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.service.HtmlService.toElements;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;

/**
 * Created by kevin on 20/07/2016
 */
@Slf4j
@Component("TF1ReplayUpdater")
@RequiredArgsConstructor
public class TF1ReplayUpdater implements Updater {

    private static final PatternExtractor CHANNEL_PROGRAM_EXTRACTOR = from(Pattern.compile("[^:]+://www.tf1.fr/([^/]+)/([^/]+)/videos.*"));
    private static final String AJAX_URL_FORMAT = "http://www.tf1.fr/ajax/%s/%s/videos?filter=%s";
    private static final String SCHEME_DEFAULT = "https:";
    private static final String DOMAIN = "https://www.tf1.fr";
    private static final String REPLAY_CATEGORY = "replay";
    private static final String ALL_CATEGORY = "all";
    private static final Set<String> TYPES = HashSet.of("replay", "vidéo", "");

    private final SignatureService signatureService;
    private final HtmlService htmlService;
    private final ImageService imageService;
    private final JsonService jsonService;

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return getHtmlFromStandardOrReplay(podcast.getUrl())
                .map(this::itemsFromHtml)
                .getOrElse(HashSet::empty);
    }

    private Set<Item> itemsFromHtml(Elements els) {
        return HashSet.ofAll(els).map(this::getItem);
    }

    private Item getItem(Element e) {
        String link = e.select(".videoLink").attr("href");
        String url = StringUtils.startsWith(link, "/") ? DOMAIN + link : link;

        return Item.builder()
                    .title(getTitle(e))
                    .description(e.select("p.stitle").text())
                    .pubDate(getDate(url))
                    .url(url)
                    .cover(getCover(e))
                .build();
    }

    private String getTitle(Element v) {
        String text = v.select("p.title").text();
        if (text.contains(" - ")) {
            return StringUtils.substringAfter(text, " - ").trim();
        }

        return text;
    }

    private Cover getCover(Element e) {
        String[] sources = e.select("source").first().attr("data-srcset").split(",");
        String url = List.of(List.of(sources).last().split(" ")).get();
        return imageService.getCoverFromURL(SCHEME_DEFAULT + url);
    }

    private ZonedDateTime getDate(String url) {
        return htmlService.get(url)
                .map(d -> d.select("script[type=application/ld+json]"))
                .map(List::ofAll)
                .getOrElse(List::empty)
                .headOption()
                .map(Element::html)
                .map(jsonService::parse)
                .map(JsonService.to(TF1ReplayItem.class))
                .map(TF1ReplayItem::getUploadDate)
                .getOrElse(ZonedDateTime::now);
    }

    @Override
    public String signatureOf(Podcast podcast) {
        // Url origine : http://www.tf1.fr/tf1/19h-live/videos
        // Url replay : http://www.tf1.fr/ajax/tf1/19h-live/videos?filter=replay
        return getHtmlFromStandardOrReplay(podcast.getUrl())
                .map(Elements::html)
                .map(signatureService::fromText)
                .getOrElse(StringUtils.EMPTY);
    }

    private Option<Elements> getHtmlFromStandardOrReplay(String url) {
        Option<Elements> replays = getElementsFrom(url, REPLAY_CATEGORY);

        if(replays.map(ArrayList::size).getOrElse(0) == 0) {
            replays = getElementsFrom(url, ALL_CATEGORY);
        }

        return replays;
    }

    private Option<Elements> getElementsFrom(String url, @NotNull  String inCategory) {
        return CHANNEL_PROGRAM_EXTRACTOR.on(url).groups()
                .map(l -> String.format(AJAX_URL_FORMAT, l.get(0), l.get(1), inCategory))
                .flatMap(url1 -> jsonService.parseUrl(url1))
                .map(JsonService.to(TF1ReplayResponse.class))
                .map(TF1ReplayResponse::getHtml)
                .map(htmlService::parse)
                .map(d -> d.select(".video"))
                .map(this::getElementsElementsFunction);
    }

    private Elements getElementsElementsFunction(Elements elements) {
        return elements
                .stream()
                .filter(e -> StringUtils.isNotEmpty(e.attr("data-id")))
                .filter(this::isReplayOrVideo)
                .collect(toElements());
    }

    private Boolean isReplayOrVideo(Element element) {
        return TYPES.contains(StringUtils.lowerCase(element.select(".uptitle strong").text()));
    }

    @Override
    public Type type() {
        return new Type("TF1Replay", "TF1 Replay");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TF1ReplayResponse {
        @Setter @Getter String html;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TF1ReplayItem {
        @Setter @Getter ZonedDateTime uploadDate;
    }
}
