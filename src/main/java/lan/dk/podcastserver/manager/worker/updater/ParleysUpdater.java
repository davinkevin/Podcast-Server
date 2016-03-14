package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 12/07/2014.
 */
@Slf4j
@Component("ParleysUpdater")
public class ParleysUpdater extends AbstractUpdater {

    public static final String PARLEYS_PATTERN = "EEE MMM dd HH:mm:ss z yyyy";
    public static final String PARLEYS_CHANNEL_API_URL = "http://api.parleys.com/api/presentations.json/%s?index=0&size=%s&text=&orderBy=date";
    public static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/%s?view=true";
    public static final String PARLEYS_ITEM_URL = "http://www.parleys.com/play/%s";
    public static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/channel/([^/]*)/.*");

    public static final String FIELD_ID = "_id";
    public static final String FIELD_COUNT = "count";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PUBLISHED_ON = "publishedOn";
    public static final String FIELD_BASE_PATH = "basePath";
    public static final String FIELD_THUMBNAIL = "thumbnail";
    public static final String FIELD_RESULTS = "results";
    public static final String FIELD_VISIBILITY = "visibility";
    public static final String FIELD_FREE = "free";
    public static final String FIELD_COMPLETED_IN = "completedIn";

    @Resource ImageService imageService;
    @Resource UrlService urlService;
    @Resource JsonService jsonService;

    public Set<Item> getItems(Podcast podcast) {
        return getParseJsonObject(podcast.getUrl(), getNumberOfItem(podcast.getUrl()))
            .map(this::getParleysPresentationResultsArray)
            .map(this::fromJsonArrayToItems)
            .orElse(Sets.newHashSet());
    }

    private Set<Item> fromJsonArrayToItems(List<JSONObject> jsonObjects) {
        return jsonObjects.stream()
                .filter(this::isFree)
                .map(elem -> elem.get(FIELD_ID))
                .map(String::valueOf)
                .map(this::getParleysItem)
                .collect(toSet());
    }


    @Override
    public String signatureOf(Podcast podcast) {
        return getParseJsonObject(podcast.getUrl(), null)
                .map(p -> {
                    p.remove(FIELD_COMPLETED_IN);
                    p.remove(FIELD_RESULTS);
                    return p;
                })
                .map(p -> signatureService.generateMD5Signature(p.toJSONString()))
                .orElse("");
    }

    private Item getParleysItem(String id) {
        return urlService
                .newURL(getItemUrl(id))
                .flatMap(jsonService::from)
                .map(jsonObject -> fromJsonToItem(jsonObject, id))
                .orElse(Item.DEFAULT_ITEM);
    }

    private Item fromJsonToItem(JSONObject responseObject, String id) {
        return Item.builder()
                .title((String) responseObject.get(FIELD_TITLE))
                .description((String) responseObject.get(FIELD_DESCRIPTION))
                .pubdate(fromParleys((String) responseObject.get(FIELD_PUBLISHED_ON)))
                .cover(imageService.getCoverFromURL(((String) responseObject.get(FIELD_BASE_PATH)).concat((String) responseObject.get(FIELD_THUMBNAIL))))
                .url(String.format(PARLEYS_ITEM_URL, id))
                .build();
    }

    private Optional<JSONObject> getParseJsonObject(String url, Integer numberOfItem) {
        return urlService
                .newURL(getParleysPresentationUrl(url, numberOfItem))
                .flatMap(jsonService::from);
    }

    @SuppressWarnings("unchecked")
    private List<JSONObject> getParleysPresentationResultsArray(JSONObject responseObject) {
        return (List<JSONObject>) responseObject.get(FIELD_RESULTS);
    }

    private String getItemUrl(String id) {
        return String.format(PARLEYS_ITEM_API_URL, id);
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

    private Integer getNumberOfItem(String url) {
        return getParseJsonObject(url, 1)
            .map(r -> nonNull(r.get(FIELD_COUNT)) ? ((Long) r.get(FIELD_COUNT)).intValue() : 100)
            .orElse(100);
    }

    private Boolean isFree(JSONObject currentObject) {
        return (Boolean) ((JSONObject) currentObject.get(FIELD_VISIBILITY)).get(FIELD_FREE);
    }

    private ZonedDateTime fromParleys(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ofPattern(PARLEYS_PATTERN, Locale.ENGLISH)); // Format : Thu Jun 26 06:34:41 UTC 2014
    }

    @Override
    public Type type() {
        return new Type("Parleys", "Parleys");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "parleys.com") ? 1 : Integer.MAX_VALUE;
    }
}
