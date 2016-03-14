package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 09/08/2014 for Podcast Server
 */
@Slf4j
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
    @Resource JsonService jsonService;

    public Set<Item> getItems(Podcast podcast) {
        Optional<Document> page = htmlService.get(podcast.getUrl());

        return page
            .map(p -> p.select(JSOUP_ITEM_SELECTOR).select("a.row"))
            .map(this::htmlToItems)
            .map(s -> {
                s.add(getCurrentPlayedItem(page.get()));
                return s;
            })
            .orElse(Sets.newHashSet());
    }

    private Set<Item> htmlToItems(Elements elements) {
        return elements.stream()
                .map(element -> getPluzzItemByUrl(element.attr("href")))
                .collect(toSet());
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
        Optional<Document> page = htmlService.get(podcast.getUrl());

        return page
            .map(p -> p.select(JSOUP_ITEM_SELECTOR))
            .map(l -> (l.size() == 0) ? page.get().html() : l.html())
            .map(signatureService::generateMD5Signature)
            .orElse(StringUtils.EMPTY);
    }

    private Item getPluzzItemByUrl(String url) {
        String pluzzId = getPluzzId(url);

        if (pluzzId.isEmpty())
            return Item.DEFAULT_ITEM;

        return getPluzzItemById(pluzzId);
    }

    private Item getPluzzItemById(String pluzzId) {
        return urlService
                .newURL(getPluzzJsonInformation(pluzzId))
                .flatMap(jsonService::from)
                .map(this::jsonToItem)
                .orElse(Item.DEFAULT_ITEM);
    }

    private Item jsonToItem(JSONObject responseObject) {

        String season = String.valueOf(responseObject.get("saison"));
        String episode = String.valueOf(responseObject.get("episode"));
        String seasonEpisode = !"null".equals(season) && !"null".equals(episode) ? " - S".concat(season).concat("E").concat(episode).concat(" - ") : " - ";

        return Item.builder()
                    .title( StringUtils.join(responseObject.get("titre").toString(), seasonEpisode,responseObject.get("sous_titre").toString()) )
                    .description( String.valueOf(responseObject.get("synopsis")))
                    .pubdate( fromPluzz(responseObject) )
                    .cover( imageService.getCoverFromURL(String.format(PLUZZ_COVER_BASE_URL, (String) responseObject.get("image"))))
                    .url( getPluzzM38uUrl((JSONArray) responseObject.get("videos")))
                .build();
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

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "pluzz.francetv.fr")
                ? 1
                : Integer.MAX_VALUE;
    }
}
