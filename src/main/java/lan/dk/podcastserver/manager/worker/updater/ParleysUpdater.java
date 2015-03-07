package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.URLUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 12/07/2014.
 */
@Component("ParleysUpdater")
public class ParleysUpdater extends AbstractUpdater {

    public static final String PARLEYS_PATTERN = "EEE MMM dd HH:mm:ss z yyyy";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String PARLEYS_CHANNEL_API_URL = "http://api.parleys.com/api/presentations.json/%s?index=0&size=%s&text=&orderBy=date";
    public static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/%s?view=true";
    public static final String PARLEYS_ITEM_URL = "http://www.parleys.com/play/%s";

    /* Patter to extract value from URL */
    public static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/channel/([^/]*)/.*");

    @Override
    public Podcast updateAndAddItems(Podcast podcast) {

        // Si le bean est valide :
        getItems(podcast).stream()
                .filter(notIn(podcast))
                .map(item -> item.setPodcast(podcast))
                .filter(item -> validator.validate(item).isEmpty())
                .forEach(podcast::add);

        return podcast;
    }


    public Set<Item> getItems(Podcast podcast) {
        Set<Item> itemSet = new HashSet<>();

        try {
            JSONObject responseObject = getParseJsonObject(podcast.getUrl(), getNumberOfItem(podcast.getUrl()));

            JSONArray resultArray = getParleysPresentationResultsArray(responseObject);

            for (Object jsonObject : resultArray) {

                JSONObject currentObject = (JSONObject) jsonObject;

                String _id = (String) currentObject.get("_id");
                if (isFree(currentObject)) {
                    itemSet.add(getParleysItem(_id));
                }
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return itemSet;
    }


    @Override
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String generateSignature(Podcast podcast) {
        try {
            JSONObject podcastRepresentation = getParseJsonObject(podcast.getUrl(), null);
            podcastRepresentation.remove("completedIn");
            podcastRepresentation.remove("results");
            return signatureService.generateMD5SignatureFromDOM(podcastRepresentation.toJSONString());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    private JSONObject getParseJsonObject(String url, Integer numberOfItem) throws IOException, ParseException {
        return (JSONObject) new JSONParser().parse(URLUtils.getReaderFromURL(getParleysPresentationUrl(url, numberOfItem)));
    }

    private JSONArray getParleysPresentationResultsArray(JSONObject responseObject) {
        return (JSONArray) responseObject.get("results");
    }

    private Item getParleysItem(String id) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject responseObject = (JSONObject) parser.parse(URLUtils.getReaderFromURL(getItemUrl(id)));

            String baseURL = (String) responseObject.get("basePath");

            return new Item()
                            .setTitle((String) responseObject.get("title"))
                            .setDescription((String) responseObject.get("description"))
                            .setPubdate(fromParleys((String) responseObject.get("publishedOn")))
                            .setCover(ImageUtils.getCoverFromURL(new URL(baseURL.concat((String) responseObject.get("thumbnail")))))
                            .setUrl(String.format(PARLEYS_ITEM_URL, id));


        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return new Item();
    }

    private String getItemUrl(String id) {
        return String.format(PARLEYS_ITEM_API_URL, id);
    }

    private String getParleysPresentationUrl(String url) {
        return getParleysPresentationUrl(url, null);
    }

    private String getParleysPresentationUrl(String url, Integer numberOfItem) {
        return String.format(PARLEYS_CHANNEL_API_URL, getParleysId(url), (numberOfItem == null) ? "1" : numberOfItem.toString());
    }

    private String getParleysId(String url) {
        // Extraction de l'id de l'emission :
        Matcher m = ID_PARLEYS_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private Integer getNumberOfItem(String url) throws IOException, ParseException {
        JSONObject responseObject = getParseJsonObject(url, 1);
        return (responseObject.get("count") != null) ? ((Long) responseObject.get("count")).intValue() : 100 ;
    }


    private Boolean isFree(JSONObject currentObject) {
        return (Boolean) ((JSONObject) currentObject.get("visibility")).get("free");
    }

    private ZonedDateTime fromParleys(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ofPattern(PARLEYS_PATTERN, Locale.ENGLISH)); // Format : Thu Jun 26 06:34:41 UTC 2014
    }
}
