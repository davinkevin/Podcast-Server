package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
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
@Component("BeInSportsUpdater")
public class BeInSportsUpdater extends AbstractUpdater {

    /* Patter to extract value from URL */
    private static final String ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE = ".*\"%s\":\"([^\"]*)\".*";
    private static final String PARAMETER_SEPARATOR = "?";
    private static final Pattern STREAM_720_URL_EXTRACTOR_PATTERN1 = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "url"));
    private static final Pattern POSTER_URL_EXTRACTOR_PATTERN = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "poster_url"));
    private static final String PROTOCOL = "http:";
    private static final String BE_IN_SPORTS_DOMAIN = PROTOCOL + "//www.beinsports.com%s";
    /* December 26, 2015 13:53 */
    private static final DateTimeFormatter BEINSPORTS_PARSER = DateTimeFormatter.ofPattern("MMMM d, y HH:mm", Locale.ENGLISH);

    @Resource HtmlService htmlService;
    @Resource ImageService imageService;

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        try {
            page = htmlService.get(podcast.getUrl());
        } catch (IOException e) {
            logger.error("IOException :", e);
            return Sets.newHashSet();
        }

        return page.select("article")
                .stream()
                .map(this::getItem)
                .collect(toSet());
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

        String javascriptCode;
        Document document;

        try {
            document = htmlService.get(urlItemBeInSport);
            javascriptCode = getJavascriptPart(htmlService.get(PROTOCOL + document.select("iframe").attr("src")).select("script"));
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error during fetch of {}", urlItemBeInSport, e);
            return Item.DEFAULT_ITEM;
        }

        return new Item()
                .setTitle(article.select("h3").first().text())
                .setDescription(article.select("h3").first().text())
                .setPubdate(getPubDateFromDescription(document))
                .setUrl(getStreamUrl(javascriptCode).orElse(null))
                .setCover(getPoster(javascriptCode).orElse(null));
    }

    private Optional<Cover> getPoster(String javascriptCode) {
        Matcher thumbnailMatcher = POSTER_URL_EXTRACTOR_PATTERN.matcher(javascriptCode);
        if (thumbnailMatcher.find()) {
            return Optional.of(imageService.getCoverFromURL(thumbnailMatcher.group(1).replace("\\", "")));
        }
        return Optional.empty();
    }
    private Optional<String> getStreamUrl(String javascriptCode) {
        Matcher matcher = STREAM_720_URL_EXTRACTOR_PATTERN1.matcher(javascriptCode);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).replace("\\", ""));
        }
        return Optional.empty();
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
        String listingUrl = podcast.getUrl();
        /* cluster_video */
        try {
            Document page = htmlService.get(listingUrl);
            return signatureService.generateMD5Signature(page.select(".cluster_video").html());
        } catch (IOException e) {
            logger.error("IOException :", e);
        }

        return StringUtils.EMPTY;
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
}
