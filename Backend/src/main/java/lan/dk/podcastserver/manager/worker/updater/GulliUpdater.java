package lan.dk.podcastserver.manager.worker.updater;

import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javaslang.collection.HashSet.collector;

/**
 * Created by kevin on 05/10/2016 for Podcast Server
 */
@Slf4j
@Component("GulliUpdater")
public class GulliUpdater extends AbstractUpdater {

    private static final Pattern FRAME_EXTRACTOR = Pattern.compile(".*\\.html\\(.<iframe.* src=\"([^\"]*)\".*");

    private final HtmlService htmlService;
    private final ImageService imageService;

    public GulliUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, HtmlService htmlService, ImageService imageService) {
        super(podcastServerParameters, signatureService, validator);
        this.htmlService = htmlService;
        this.imageService = imageService;
    }

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return htmlService.get(podcast.getUrl())
                .map(d -> d.select("div.all-videos ul li.col-md-3"))
                .map(this::asSet)
                .map(HashSet::toJavaSet)
                .getOrElse(java.util.HashSet::new);
    }

    private HashSet<Item> asSet(Elements elements) {
        return elements.stream()
                .map(this::findDetailsInFromPage)
                .collect(collector());
    }

    private Item findDetailsInFromPage(Element e) {
        return Option.of(e.select("a").first())
            .map(elem -> elem.attr("href"))
            .flatMap(htmlService::get)
            .flatMap(Option::of)
            .map(d -> d.select(".bloc_streaming").first())
            .flatMap(this::htmlToItem)
            .flatMap(Option::of)
            .map(i -> i.setCover(getCover(e)))
            .getOrElse(Item.DEFAULT_ITEM);
    }

    private Option<Item> htmlToItem(Element block) {
        return List.ofAll(block.select("script"))
                .find(e -> e.html().contains("iframe"))
                .map(Element::html)
                .map(FRAME_EXTRACTOR::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .map(url -> Item.builder()
                    .title(block.select(".episode_title").text())
                    .description(block.select(".description").text())
                    .url(url)
                    .pubDate(ZonedDateTime.now())
                .build());
    }

    private Cover getCover(Element block) {
        return Option.of(block)
                .map(e -> e.select("img").attr("src"))
                .map(imageService::getCoverFromURL)
                .getOrElse(Cover.DEFAULT_COVER);
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return htmlService.get(podcast.getUrl())
                .map(d -> d.select("div.all-videos ul").first())
                .map(Element::html)
                .map(signatureService::generateMD5Signature)
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
