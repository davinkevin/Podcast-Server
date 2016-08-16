package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lan.dk.podcastserver.service.HtmlService.toElements;

/**
 * Created by kevin on 20/07/2016
 */
@Slf4j
@Component("TF1ReplayUpdater")
public class TF1ReplayUpdater extends AbstractUpdater {

    private static final Pattern CHANNEL_PROGRAM_EXTRACTOR = Pattern.compile("[^:]+://www.tf1.fr/([^/]+)/([^/]+)/videos.*");
    private static final String AJAX_URL_FORMAT = "http://www.tf1.fr/ajax/%s/%s/videos?filter=%s";
    private static final String SCHEME_DEFAULT = "https:";
    private static final String DOMAIN = "http://www.tf1.fr";
    private static final String REPLAY_CATEGORY = "replay";
    private static final String ALL_CATEGORY = "all";
    private static final Function<DocumentContext, TF1ReplayResponse> EXTRACT_IN_TF1_REPLAY_RESPONSE = d -> d.read("$", TF1ReplayResponse.class);
    private static final javaslang.collection.Set<String> TYPES = javaslang.collection.HashSet.of("replay", "vidéo");

    @Resource HtmlService htmlService;
    @Resource ImageService imageService;
    @Resource JsonService jsonService;

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return getHtmlFromStandardOrReplay(podcast.getUrl())
                    .map(this::itemsFromHtml)
                .getOrElse(Sets::newHashSet);
    }

    private Set<Item> itemsFromHtml(Elements v) {
        return v.stream().map(this::getItem).collect(Collectors.toSet());
    }

    private Item getItem(Element v) {
        return Item.builder()
                    .title(getTitle(v))
                    .description(v.select("p.stitle").text())
                    .pubDate(getDate(v))
                    .url(DOMAIN + v.select(".videoLink").attr("href"))
                    .cover(getCover(v))
                .build();
    }

    private String getTitle(Element v) {
        String text = v.select("p.title").text();
        if (text.contains("-")) {
            return StringUtils.substringAfter(text, "-").trim();
        }

        return text;
    }

    private Cover getCover(Element v) {
        String[] sources = v.select("source").first().attr("data-srcset").split(",");
        String[] source = sources[sources.length - 1].split(" ");
        return imageService.getCoverFromURL(SCHEME_DEFAULT + source[0]);
    }

    private ZonedDateTime getDate(Element v) {
        return v
                .select(".momentDate")
                .stream()
                    .filter(e -> !e.hasAttr("data-format"))
                    .findFirst()
                .map(e -> e.attr("data-date"))
                .map(ZonedDateTime::parse)
                .orElse(null);
    }

    @Override
    public String signatureOf(Podcast podcast) {
        // Url origine : http://www.tf1.fr/tf1/19h-live/videos
        // Url replay : http://www.tf1.fr/ajax/tf1/19h-live/videos?filter=replay
        return getHtmlFromStandardOrReplay(podcast.getUrl())
            .map(Elements::html)
            .map(signatureService::generateMD5Signature)
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
        Matcher matcher = CHANNEL_PROGRAM_EXTRACTOR.matcher(url);

        if (!matcher.find())
            return Option.none();

        return Option
                .of(String.format(AJAX_URL_FORMAT, matcher.group(1), matcher.group(2), inCategory))
                .flatMap(jsonService::parseUrl)
                .map(EXTRACT_IN_TF1_REPLAY_RESPONSE)
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
}
