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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 12/07/2014.
 */
@Component("ParleysUpdater")
@Scope("prototype")
public class ParleysUpdater extends AbstractUpdater {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String PARLEYS_CHANNEL_API_URL = "http://api.parleys.com/api/presentations.json/{ID_VIDEO}?index=0&size=100&text=&orderBy=date";
    public static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/{ID_VIDEO}?view=true";
    public static final String PARLEYS_ITEM_URL = "http://www.parleys.com/play/{ID_VIDEO}";

    /* Patter to extract value from URL */
    public static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/channel/([^/]*)/.*");

    @Override
    public Podcast updateFeed(Podcast podcast) {
        JSONParser parser = new JSONParser();


        try {
            JSONObject responseObject = (JSONObject) getParseJsonObject(podcast.getUrl());

            JSONArray resultArray = getParleysPresentationResultsArray(responseObject);

            for (Object jsonObject : resultArray) {

                JSONObject currentObject = (JSONObject) jsonObject;

                String _id = (String) currentObject.get("_id");
                Item item = getParleysItem(_id);

                logger.debug(item.toString());

                if (!podcast.containsItem(item)) {

                    // Si le bean est valide :
                    item.setPodcast(podcast);
                    Set<ConstraintViolation<Item>> constraintViolations = validator.validate( item );
                    if (constraintViolations.isEmpty()) {
                        podcast.getItems().add(item);
                    } else {
                        logger.error(constraintViolations.toString());
                    }
                }

            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return podcast;
    }

    private JSONObject getParseJsonObject(String url) throws IOException, ParseException {
        return (JSONObject) new JSONParser().parse(URLUtils.getReaderFromURL(getParleysPresentationUrl(url)));
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
                            .setPubdate(DateUtils.parleysToTimeStamp((String) responseObject.get("publishedOn")))
                            .setCover(ImageUtils.getCoverFromURL(new URL(baseURL.concat((String) responseObject.get("thumbnail")))))
                            .setUrl(PARLEYS_ITEM_URL.replace("{ID_VIDEO}", id));


        } catch (IOException | ParseException | java.text.ParseException e) {
            e.printStackTrace();
        }

        return new Item();
    }

    private String getItemUrl(String id) {
        return PARLEYS_ITEM_API_URL.replace("{ID_VIDEO}", id);
    }

    @Override
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String signaturePodcast(Podcast podcast) {
        return DigestUtils.generateMD5SignatureFromUrl(getParleysPresentationUrl(podcast.getUrl()));
    }

    private String getParleysPresentationUrl(String url) {
        return PARLEYS_CHANNEL_API_URL.replace("{ID_VIDEO}", getParleysId(url));
    }

    private String getParleysId(String url) {
        // Extraction de l'id de l'emission :
        Matcher m = ID_PARLEYS_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
}
