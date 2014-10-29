package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.DigestUtils;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.URLUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by kevin on 09/08/2014.
 */
@Component("PluzzUpdater")
@Scope("prototype")
public class PluzzUpdater extends AbstractUpdater {

    public static final String JSOUP_ITEM_SELECTOR = "#autreEmission";
    public static final String PLUZZ_INFORMATION_URL = "http://webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s&catalogue=Pluzz";
    public static final String PLUZZ_COVER_BASE_URL = "http://refonte.webservices.francetelevisions.fr%s";
    //PATTERN :
    public static Pattern ID_PLUZZ_PATTERN = Pattern.compile(".*,([0-9]*).html");

    @Override
    public Podcast updateAndAddItems(Podcast podcast) {


        // Si le bean est valide :
        getItems(podcast)
                .stream()
                .filter(item -> !podcast.getItems().contains(item))
                .forEach(item -> {

                    // Si le bean est valide :
                    item.setPodcast(podcast);
                    Set<ConstraintViolation<Item>> constraintViolations = validator.validate(item);
                    if (constraintViolations.isEmpty()) {
                        podcast.getItems().add(item);
                    } else {
                        logger.error(constraintViolations.toString());
                    }
                });

        return podcast;
    }

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        String listingUrl = podcast.getUrl();
        Set<Item> itemList = new HashSet<>();
        try {

            Connection.Response response = Jsoup.connect(listingUrl)
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.fr")
                    .execute();
            page = response.parse();


            //get from current page, for the first of the panel
            itemList.add(getPluzzItem(page.select("meta[property=og:url]").attr("content")));

            // get from right panel
            Elements listOfEpisodes = page.select(JSOUP_ITEM_SELECTOR);
            itemList.addAll(listOfEpisodes.select("a.row")
                    .stream()
                    .map(element -> getPluzzItem(element.attr("href")))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }
        return itemList;

    }


    @Override
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String generateSignature(Podcast podcast) {
        Document page;
        String listingUrl = podcast.getUrl();
        try {

            Connection.Response response = Jsoup.connect(listingUrl)
                    .timeout(5000)
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.fr")
                    .execute();
            page = response.parse();

            Elements listOfItem = page.select(JSOUP_ITEM_SELECTOR);

            return DigestUtils.generateMD5SignatureFromDOM((listOfItem.size() == 0) ? page.html() : listOfItem.html());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }
        return "";
    }

    private Item getPluzzItem(String url) {
        String pluzzId = getPluzzId(url);

        if (pluzzId.isEmpty())
            return new Item();

        JSONParser parser = new JSONParser();
        try {
            logger.debug(getPluzzJsonInformation(pluzzId));
            JSONObject responseObject = (JSONObject) parser.parse(URLUtils.getReaderFromURL(getPluzzJsonInformation(pluzzId)));

            String seasonEpisode = " - ";
            try {
                String season = String.valueOf(responseObject.get("saison"));
                String episode = String.valueOf(responseObject.get("episode"));
                if (!"null".equals(season) && !"null".equals(episode))
                    seasonEpisode = " - S".concat(season).concat("E").concat(episode).concat(" - ");
            } catch (Exception e) {
                logger.error("Problème sur Saison / Episode", e);
            }

            Item itemToReturn = new Item()
                                    .setTitle(responseObject.get("titre").toString().concat(seasonEpisode).concat(responseObject.get("sous_titre").toString()))
                                    .setDescription(responseObject.get("synopsis").toString())
                                    .setPubdate( DateUtils.fromPluzz((Long) ((JSONObject) responseObject.get("diffusion")).get("timestamp")) )
                                    .setCover(ImageUtils.getCoverFromURL(new URL(String.format(PLUZZ_COVER_BASE_URL, (String) responseObject.get("image")))))
                                    .setUrl(getPluzzM38uUrl((JSONArray) responseObject.get("videos")));



            logger.debug(itemToReturn.toString());
            return itemToReturn;

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }


        return new Item();
    }

    private String getPluzzM38uUrl(JSONArray videosArray) {
        for (Object video : videosArray) {
            JSONObject jsonVideo = (JSONObject) video;
            if (jsonVideo.get("format") != null && ((String)jsonVideo.get("format")).contains("m3u8")) {
                return URLUtils.getM3U8UrlFormMultiStreamFile((String) jsonVideo.get("url"));
            }
        }
        return "";
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
}
