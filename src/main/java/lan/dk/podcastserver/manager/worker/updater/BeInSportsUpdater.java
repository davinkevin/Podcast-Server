package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.utils.ImageUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 22/02/2014.
 *
 * Utilisation de referrer et User-Agent : http://stackoverflow.com/questions/6581655/jsoup-useragent-how-to-set-it-right
 *
 */
@Component("BeInSportsUpdater")
public class BeInSportsUpdater extends AbstractUpdater {

    /* Patter to extract value from URL */
    private static final String ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE = ".*\"%s\": \"([^\"]*)\".*";
    private static final String PARAMETER_SEPARATOR = "?";
    private static final String EPISODE_LISTING_URL = "http://www.beinsports.fr/ajax/filter-videos/siteSection/replay/filterBySelect/%s/ajaxSection/integrales";
    private static final String VIDEO_ARTICLE_URL_FORMAT = "http://www.beinsports.fr/ajax/swap-video/article/%s";
    private static final Pattern DATE_PATTERN = Pattern.compile(".*[(]([^)]*)[)].*");
    private static final Pattern STREAM_HLS_URL_EXTRACTOR_PATTERN1 = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "stream_hls_url"));
    private static final Pattern THUMB_NAIL_EXTRACTOR_PATTERN = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "thumbnail_large_url"));

    @Resource JdomService jdomService;
    @Resource HtmlService htmlService;

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        Set<Item> itemSet = new HashSet<>();
        String listingUrl = getListingUrl(podcast);
        try {

            Connection.Response response = htmlService.connectWithDefault(listingUrl)
                    .execute();
            page = response.parse();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
            return itemSet;
        }

        for(Element article : page.select("article")) {
            Item item = new Item()
                    .setTitle(article.select("h4").first().text())
                    .setDescription(article.select("h4").first().text())
                    .setPubdate(getPubDateFromDescription(article.select("h4").first().text()));
            
            item = getDetailOfItemByXML(item, StringUtils.substringAfterLast(article.select("a").first().attr("href"), "/"));
            itemSet.add(item);
        }

        return itemSet;
    }

    private ZonedDateTime getPubDateFromDescription(String title) {
        /* L'Expresso (26/02) - 1\u00e8re partie */
        Matcher m = DATE_PATTERN.matcher(title);
        
        if (!m.find()) {
            return null;
        }
        
        String dayMonth = m.group(1);
        return ZonedDateTime
                .now()
                .withMonth(Integer.valueOf(StringUtils.substringAfter(dayMonth, "/")))
                .withDayOfMonth(Integer.valueOf(StringUtils.substringBefore(dayMonth, "/")))
                .withHour(8)
                .withMinute(0);
    }


    private Item getDetailOfItemByXML(Item item, String urlItemBeInSport) {
        String javascriptCode;
        try {
            Connection.Response response = htmlService.connectWithDefault(String.format(VIDEO_ARTICLE_URL_FORMAT, urlItemBeInSport))
                    .execute();
            Document articlePage = response.parse();
            String apiItemUrl = articlePage.select("iframe").attr("src");

            response = htmlService.connectWithDefault(apiItemUrl)
                    .execute();
            javascriptCode = getJavascriptPart(response.parse().select("script"));
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error during fetch of {}", String.format(VIDEO_ARTICLE_URL_FORMAT, urlItemBeInSport), e);
            return new Item();
        }


        Matcher matcher = STREAM_HLS_URL_EXTRACTOR_PATTERN1.matcher(javascriptCode);
        if (matcher.find()) {
            item.setUrl(matcher.group(1));
        }

        Matcher thumNailematcher = THUMB_NAIL_EXTRACTOR_PATTERN.matcher(javascriptCode);
        if (thumNailematcher.find()) {
            try {
                item.setCover(ImageUtils.getCoverFromURL(thumNailematcher.group(1)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return item;
    }

    private String getJavascriptPart(Elements tagScripts) {
        return tagScripts.stream()
                .map(Element::data)
                .filter(data -> data.contains("stream_hls_url"))
                .findFirst()
                .orElse("");
    }

    @Override
    public String signatureOf(Podcast podcast) {
        String listingUrl = getListingUrl(podcast);

        if (!listingUrl.equals("")) {
            return signatureService.generateSignatureFromURL(podcast.getUrl());
        } else {
            return "";
        }
    }

    private String getListingUrl(Podcast podcast) {
        String idShow = StringUtils.substringAfterLast(podcast.getUrl(), "/");
        return String.format(EPISODE_LISTING_URL, idShow);
    }
    
    public Boolean podcastContains(Podcast podcast, Item item) {
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
