package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 12/07/2014.
 */
@Slf4j
@Component("ParleysUpdater")
public class ParleysUpdater extends AbstractUpdater {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
    private static final String PARLEYS_CHANNEL_API_URL = "http://api.parleys.com/api/presentations.json/%s?index=0&size=%s&text=&orderBy=date";
    private static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/%s?view=true";
    private static final String PARLEYS_ITEM_URL = "http://www.parleys.com/play/%s";
    private static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/channel/([^/]*)");

    private static final TypeRef<List<ParleysResult>> LIST_PARLEYS_RESULTS = new TypeRef<List<ParleysResult>>(){};

    @Resource ImageService imageService;
    @Resource UrlService urlService;
    @Resource JsonService jsonService;

    public Set<Item> getItems(Podcast podcast) {
        return getParseJsonObject(podcast.getUrl(), getNumberOfItem(podcast.getUrl()))
            .map(d -> d.read("results", LIST_PARLEYS_RESULTS))
            .map(this::fromJsonArrayToItems)
            .orElse(Sets.newHashSet());
    }

    private Set<Item> fromJsonArrayToItems(List<ParleysResult> results) {
        return results.stream()
                .filter(ParleysResult::isFree)
                .map(ParleysResult::get_id)
                .map(this::getParleysItem)
                .collect(toSet());
    }


    @Override
    public String signatureOf(Podcast podcast) {
        return getParseJsonObject(podcast.getUrl(), null)
                .map(d -> d.read("$", ParleysResponse.class))
                .map(p -> signatureService.generateMD5Signature(p.toString()))
                .orElse("");
    }

    private Item getParleysItem(String id) {
        return urlService
                .newURL(getItemUrl(id))
                .flatMap(jsonService::parse)
                .map(d -> d.read("$", ParleysResult.class))
                .map(this::fromJsonToItem)
                .orElse(Item.DEFAULT_ITEM);
    }

    private Item fromJsonToItem(ParleysResult result) {
        return Item.builder()
                .title(result.getTitle())
                .description(result.getDescription())
                .pubDate(result.publishedOn())
                .cover(imageService.getCoverFromURL(result.coverUrl()))
                .url(result.url())
                .build();
    }

    private Optional<DocumentContext> getParseJsonObject(String url, Integer numberOfItem) {
        return urlService
                .newURL(getParleysPresentationUrl(url, numberOfItem))
                .flatMap(jsonService::parse);
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
            .map(d -> d.read("$", ParleysResponse.class))
            .map(ParleysResponse::getCount)
            .orElse(100);
    }

    @Override
    public Type type() {
        return new Type("Parleys", "Parleys");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "parleys.com") ? 1 : Integer.MAX_VALUE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParleysResponse {
        @Getter @Setter private Integer index;
        @Getter @Setter private Integer size;
        @Getter @Setter private Integer count;
        @Getter @Setter private String extra;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParleysResult {
        @Getter @Setter private String _id;
        @Getter @Setter private Visibility visibility;
        @Getter @Setter private String title;
        @Getter @Setter private String description;
        @Setter private String publishedOn;
        @Setter private String basePath;
        @Setter private String thumbnail;

        Boolean isFree() { return this.visibility.getFree(); }

        String coverUrl() {
            return this.basePath + this.thumbnail;
        }

        ZonedDateTime publishedOn() {
            return ZonedDateTime.parse(this.publishedOn, formatter); // Format : Thu Jun 26 06:34:41 UTC 2014
        }

        String url() {
            return String.format(PARLEYS_ITEM_URL, this._id);
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Visibility {
            @Getter @Setter private Boolean free;
        }
    }
}
