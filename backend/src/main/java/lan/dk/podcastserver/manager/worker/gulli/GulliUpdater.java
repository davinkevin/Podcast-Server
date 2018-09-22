package lan.dk.podcastserver.manager.worker.gulli;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;
import static io.vavr.API.Option;

/**
 * Created by kevin on 05/10/2016 for Podcast Server
 */
@Slf4j
@Component("GulliUpdater")
@RequiredArgsConstructor
public class GulliUpdater implements Updater {

    private static final PatternExtractor FRAME_EXTRACTOR = from(Pattern.compile(".*\\.html\\(.*<iframe.* src=\"([^\"]*)\".*"));

    private final SignatureService signatureService;
    private final HtmlService htmlService;
    private final ImageService imageService;

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return htmlService.get(podcast.getUrl())
                .map(d -> d.select("div.all-videos ul li.col-md-3"))
                .filter(Objects::nonNull)
                .map(this::asItemsSet)
                .getOrElse(HashSet::empty);
    }

    private Set<Item> asItemsSet(Elements elements) {
        return HashSet.ofAll(elements)
                .map(this::findDetailsInFromPage);
    }

    private Item findDetailsInFromPage(Element e) {
        return Option(e.select("a").first())
            .map(elem -> elem.attr("href"))
            .flatMap(htmlService::get)
            .map(d -> d.select(".bloc_streaming").first())
            .flatMap(this::htmlToItem)
            .map(i -> i.setCover(getCover(e)))
            .getOrElse(Item.DEFAULT_ITEM);
    }

    private Option<Item> htmlToItem(Element block) {
        return List.ofAll(block.select("script"))
                .find(e -> e.html().contains("iframe"))
                .map(Element::html)
                .map(FRAME_EXTRACTOR::on)
                .flatMap(m -> m.group(1))
                .map(url -> Item.builder()
                    .title(block.select(".episode_title").text())
                    .description(block.select(".description").text())
                    .url(url)
                    .pubDate(ZonedDateTime.now())
                .build());
    }

    private Cover getCover(Element block) {
        return Option(block)
                .map(e -> e.select("img").attr("src"))
                .map(imageService::getCoverFromURL)
                .getOrElse(Cover.DEFAULT_COVER);
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return htmlService.get(podcast.getUrl())
                .map(d -> d.select("div.all-videos ul").first())
                .filter(Objects::nonNull)
                .map(Element::html)
                .map(signatureService::fromText)
                .getOrElse(StringUtils.EMPTY);
    }

    @Override
    public Type type() {
        return new Type("Gulli", "Gulli");
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("replay.gulli.fr") ? 1 : Integer.MAX_VALUE;
    }
}
