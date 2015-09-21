package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
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
    private static final String ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE = ".*\"%s\":\"([^\"]*)\".*";
    private static final String PARAMETER_SEPARATOR = "?";
    private static final Pattern STREAM_720_URL_EXTRACTOR_PATTERN1 = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "url"));
    private static final Pattern POSTER_URL_EXTRACTOR_PATTERN = Pattern.compile(String.format(ATTRIBUTE_EXTRACTOR_FROM_JAVASCRIPT_VALUE, "poster_url"));
    private static final String beInSportsDomain = "http://www.beinsports.com/%s";

    @Resource JdomService jdomService;
    @Resource HtmlService htmlService;
    @Resource ImageService imageService;

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        Set<Item> itemSet = new HashSet<>();
        String listingUrl = podcast.getUrl();
        try {

            Connection.Response response = htmlService.connectWithDefault(listingUrl).execute();
            page = response.parse();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
            return itemSet;
        }

        for(Element article : page.select("article")) {
            Item item = new Item()
                    .setTitle(article.select("h3").first().text())
                    .setDescription(article.select("h3").first().text())
                    .setPubdate(getPubDateFromDescription(article.select("time").first().attr("datetime")));
            
            item = getDetailsByJavascript(item, String.format(beInSportsDomain, article.select("a").first().attr("data-url")));
            itemSet.add(item);
        }

        return itemSet;
    }

    private ZonedDateTime getPubDateFromDescription(String dateString) {
        /* 2015-09-04T04:26:18+00:00 */
        return ZonedDateTime.parse(dateString);
    }


    private Item getDetailsByJavascript(Item item, String urlItemBeInSport) {
        String javascriptCode;
        try {
            Connection.Response response = htmlService.connectWithDefault(urlItemBeInSport)
                    .execute();
            Document articlePage = response.parse();
            String apiItemUrl = articlePage.select("iframe").attr("src");

            response = htmlService.connectWithDefault(apiItemUrl)
                    .execute();
            javascriptCode = getJavascriptPart(response.parse().select("script"));
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error during fetch of {}", urlItemBeInSport, e);
            return new Item();
        }


        Matcher matcher = STREAM_720_URL_EXTRACTOR_PATTERN1.matcher(javascriptCode);
        if (matcher.find()) {
            item.setUrl(matcher.group(1).replace("\\", ""));
        }

        Matcher thumNailematcher = POSTER_URL_EXTRACTOR_PATTERN.matcher(javascriptCode);
        if (thumNailematcher.find()) {
            item.setCover(imageService.getCoverFromURL(thumNailematcher.group(1).replace("\\", "")));
        }

        return item;
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
            Document page = htmlService.connectWithDefault(listingUrl).execute().parse();
            return signatureService.generateMD5Signature(page.select(".cluster_video").html());
        } catch (IOException e) {
            logger.error("IOException :", e);
        }

        return "";
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
