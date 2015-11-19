package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.UrlService;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

/**
 * Created by kevin on 09/08/2014 for Podcast Server
 */
@Component("PluzzUpdater")
public class PluzzUpdater extends AbstractUpdater {

    public static final String JSOUP_ITEM_SELECTOR = "#player-memeProgramme";
    public static final String PLUZZ_INFORMATION_URL = "http://webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s&catalogue=Pluzz";
    public static final String PLUZZ_COVER_BASE_URL = "http://refonte.webservices.francetelevisions.fr%s";
    //PATTERN :
    public static Pattern ID_PLUZZ_PATTERN = Pattern.compile(".*,([0-9]*).html");
    public static Pattern ID_PLUZZ_MAIN_PAGE_PATTERN = Pattern.compile(".*/referentiel_emissions/([^/]*)/.*");

    @Resource HtmlService htmlService;
    @Resource ImageService imageService;
    @Resource UrlService urlService;

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        String listingUrl = podcast.getUrl();
        try {
            page = htmlService.connectWithDefault(listingUrl).execute().parse();
        } catch (IOException e) {
            logger.error("IOException :", e);
            return new HashSet<>();
        }

        //get from current page, for the first of the panel
        Set<Item> items = new HashSet<>();
        items.add(getCurrentPlayedItem(page));

        // get from right panel
        Elements listOfEpisodes = page.select(JSOUP_ITEM_SELECTOR);
        List<Item> itemList = listOfEpisodes.select("a.row")
                .stream()
                .map(element -> getPluzzItemByUrl(element.attr("href")))
                .collect(toList());
        items.addAll(
                itemList
        );

        return items;
    }

    private ZonedDateTime fromPluzz(JSONObject responseObject){
        if (isNull(responseObject) || isNull(responseObject.get("diffusion")) || isNull(((JSONObject) responseObject.get("diffusion")).get("timestamp"))) {
            return null;
        }

        return ZonedDateTime.ofInstant(Instant.ofEpochSecond((Long) ((JSONObject) responseObject.get("diffusion")).get("timestamp")), ZoneId.of("Europe/Paris"));
    }


    private Item getCurrentPlayedItem(Document page) {
        String urlContainingId = page.select("meta[name=og:image]").attr("content");
        Matcher m = ID_PLUZZ_MAIN_PAGE_PATTERN.matcher(urlContainingId);
        if (!m.find()) {
            return Item.DEFAULT_ITEM;
        }
        return getPluzzItemById(m.group(1));
    }


    @Override
    public String signatureOf(Podcast podcast) {
        Document page;
        String listingUrl = podcast.getUrl();
        try {
            page = htmlService.connectWithDefault(listingUrl).execute().parse();
            Elements listOfItem = page.select(JSOUP_ITEM_SELECTOR);

            return signatureService.generateMD5Signature((listOfItem.size() == 0) ? page.html() : listOfItem.html());
        } catch (IOException e) {
            logger.error("IOException :", e);
        }
        return "";
    }

    private Item getPluzzItemByUrl(String url) {
        String pluzzId = getPluzzId(url);

        if (pluzzId.isEmpty())
            return Item.DEFAULT_ITEM;

        return getPluzzItemById(pluzzId);
    }

    private Item getPluzzItemById(String pluzzId) {
        JSONParser parser = new JSONParser();
        try {
            logger.debug(getPluzzJsonInformation(pluzzId));
            JSONObject responseObject = (JSONObject) parser.parse(urlService.getReaderFromURL(getPluzzJsonInformation(pluzzId)));

            String season = String.valueOf(responseObject.get("saison"));
            String episode = String.valueOf(responseObject.get("episode"));
            String seasonEpisode = !"null".equals(season) && !"null".equals(episode) ? " - S".concat(season).concat("E").concat(episode).concat(" - ") : " - ";

            Item itemToReturn = new Item()
                    .setTitle( StringUtils.join(responseObject.get("titre").toString(), seasonEpisode,responseObject.get("sous_titre").toString()) )
                    .setDescription( String.valueOf(responseObject.get("synopsis")))
                    .setPubdate( fromPluzz(responseObject) )
                    .setCover( imageService.getCoverFromURL(String.format(PLUZZ_COVER_BASE_URL, (String) responseObject.get("image"))))
                    .setUrl( getPluzzM38uUrl((JSONArray) responseObject.get("videos")));

            logger.debug(itemToReturn.toString());
            return itemToReturn;
        } catch (IOException | ParseException e) {
            logger.error("Error during getPluzzItemById", e);
        }

        return Item.DEFAULT_ITEM;
    }

    @SuppressWarnings("unchecked")
    private String getPluzzM38uUrl(JSONArray videosArray) {
        return ((List<JSONObject>) videosArray)
                .stream()
                .filter(hasFormatWithM3U())
                .map(o -> ((String) o.get("url")))
                .findFirst()
                .map(urlService::getM3U8UrlFormMultiStreamFile)
                .orElse("");
    }

    private Predicate<JSONObject> hasFormatWithM3U() {
        return video -> video.get("format") != null && ((String) video.get("format")).contains("m3u8");
    }

    private String getPluzzJsonInformation(String pluzzId) {
        return String.format(PLUZZ_INFORMATION_URL, pluzzId);
    }

    private String getPluzzId(String url) {
        Matcher m = ID_PLUZZ_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    @Override
    public Type type() {
        return new Type("Pluzz", "Pluzz");
    }
}
