package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 22/02/2014.
 *
 * Utilisation de referrer et User-Agent : http://stackoverflow.com/questions/6581655/jsoup-useragent-how-to-set-it-right
 *
 */
@Slf4j
@Component("BeInSportsUpdater")
public class BeInSportsUpdater extends AbstractUpdater {

    /* Patter to extract value from URL */
    private static final String ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE = ".*\"%s\":\"([^\"]*)\".*";
    private static final String PARAMETER_SEPARATOR = "?";
    private static final Pattern EXTRACTOR_ID_OF_DAILYMOTION_EMBEDED_URL = Pattern.compile(".*/(.*)$");
    private static final Pattern POSTER_URL_EXTRACTOR_PATTERN = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "poster_url"));
    private static final String PROTOCOL = "http:";
    private static final String BE_IN_SPORTS_DOMAIN = PROTOCOL + "//www.beinsports.com%s";
    private static final String DAILYMOTION_PREFIX_URL = "http://www.dailymotion.com/video/%s";
    /* December 26, 2015 13:53 */
    private static final DateTimeFormatter BEINSPORTS_PARSER = DateTimeFormatter.ofPattern("MMMM d, y HH:mm", Locale.ENGLISH);

    final HtmlService htmlService;
    final ImageService imageService;

    public BeInSportsUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, HtmlService htmlService, ImageService imageService) {
        super(podcastServerParameters, signatureService, validator);
        this.htmlService = htmlService;
        this.imageService = imageService;
    }

    public Set<Item> getItems(Podcast podcast) {
        return htmlService
                .get(podcast.getUrl())
                .map(p -> p.select("article"))
                .map(this::convertHtmlToItems)
                .getOrElse(Sets.newHashSet());
    }

    private Set<Item> convertHtmlToItems(Elements htmlItems) {
        return htmlItems.stream().map(this::getItem).collect(toSet());
    }

    private ZonedDateTime getPubDateFromDescription(Element article) {
        Element time = article.select("time").first();
        /* December 26, 2015 13:53 */
        return isNull(time)
                ? ZonedDateTime.now()
                : ZonedDateTime.of(LocalDateTime.parse(time.attr("datetime"), BEINSPORTS_PARSER), ZoneId.systemDefault());
    }


    private Item getItem(Element article) {
        String urlItemBeInSport = String.format(BE_IN_SPORTS_DOMAIN, article.select("a").first().attr("data-url"));
        Option<Document> document = htmlService.get(urlItemBeInSport);

        return document
                .map(this::getDailymotionIframeUrl)
                .flatMap(htmlService::get)
                .map(d -> d.select("script"))
                .map(this::getJavascriptPart)
                .map(html -> this.convertHtmlToItem(article, html, document.get()))
                .getOrElse(Item.DEFAULT_ITEM);
    }

    private String getDailymotionIframeUrl(Document d) {
        return PROTOCOL + d.select("iframe").attr("src"); // http://www.dailymotion.com/embed/video/k5bdvgBiDPV15Gi5vcU
    }

    private Item convertHtmlToItem(Element article, String javascriptCode, Document document) {
        return Item.builder()
                .title(article.select("h3").first().text())
                .description(article.select("h3").first().text())
                .pubDate(getPubDateFromDescription(document))
                .url(getStreamUrl(document).orElse(null))
                .cover(getPoster(javascriptCode).orElse(null))
                .build();
    }

    private Optional<Cover> getPoster(String javascriptCode) {
        Matcher thumbnailMatcher = POSTER_URL_EXTRACTOR_PATTERN.matcher(javascriptCode);
        if (thumbnailMatcher.find()) {
            return Optional.of(imageService.getCoverFromURL(thumbnailMatcher.group(1).replace("\\", "")));
        }
        return Optional.empty();
    }

    private Optional<String> getStreamUrl(Document document) {
        String dailymotionEmbedded = getDailymotionIframeUrl(document);
        Matcher matcher = EXTRACTOR_ID_OF_DAILYMOTION_EMBEDED_URL.matcher(dailymotionEmbedded);

        return matcher.find() ? Optional.of(String.format(DAILYMOTION_PREFIX_URL, matcher.group(1))) : Optional.empty();
    }

    private String getJavascriptPart(Elements tagScripts) {
        return tagScripts.stream()
                .map(Element::data)
                .filter(data -> data.contains("720"))
                .findFirst()
                .orElse("");
    }

    @Override
    public String signatureOf(Podcast podcast) {
        /* cluster_video */
        return htmlService
                .get(podcast.getUrl())
                .map(p -> p.select(".cluster_video").html())
                .map(signatureService::generateMD5Signature)
                .getOrElse(StringUtils.EMPTY);
    }

    private Boolean podcastContains(Podcast podcast, Item item) {
        if (item.getUrl() == null)
            return false;
        
        String itemToFindSimplifiedUrl = StringUtils.substringBefore(item.getUrl(), PARAMETER_SEPARATOR);

        return podcast.getItems().stream()
                .map(Item::getUrl)
                .map(url -> StringUtils.substringBefore(url, PARAMETER_SEPARATOR))
                .anyMatch(itemToFindSimplifiedUrl::equals);
    }


    public Predicate<Item> notIn(Podcast podcast) {
        return item -> !podcastContains(podcast, item);
    }

    @Override
    public Type type() {
        return new Type("BeInSports", "Be In Sports");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "beinsports.com") ? 1 : Integer.MAX_VALUE;
    }
}
