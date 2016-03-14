package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component("CanalPlusUpdater")
public class CanalPlusUpdater extends AbstractUpdater {

    public static final String CANALPLUS_PATTERN = "dd/MM/yyyy-HH:mm:ss";
    public static final String FRONT_TOOLS_URL_PATTERN = "http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=%d&ztid=%d&nbPlusVideos0=1%s";
    public static final String XML_INFORMATION_PATTERN = "http://service.canal-plus.com/video/rest/getVideos/cplus/%d";
    public static final Pattern ID_EXTRACTOR = Pattern.compile("^loadVideoHistory\\('[0-9]*','[0-9]*','[0-9]*','([0-9]*)','([0-9]*)', '[0-9]*', '[^']*'\\);");
    public static final Pattern NB_PLUS_VIDEOS_PATTERN = Pattern.compile(".*nbPlusVideos([0-9])=[1-9].*");
    public static final Pattern TABS_EXTRACTOR = Pattern.compile(".*tab=1-([0-9]*).*");
    public static final String FIELD_TITRAGE = "TITRAGE";
    public static final String FIELD_TITRE = "TITRE";
    public static final String FIELD_PUBLICATION = "PUBLICATION";
    public static final String FIELD_DATE = "DATE";
    public static final String FIELD_HEURE = "HEURE";
    public static final String FIELD_IMAGES = "IMAGES";
    public static final String FIELD_GRAND = "GRAND";
    public static final String FIELD_SOUS_TITRE = "SOUS_TITRE";
    public static final String FIELD_VIDEOS = "VIDEOS";
    public static final String FIELD_QUALITY_HLS = "HLS";
    public static final String FIELD_QUALITY_HD = "HD";
    public static final String SELECTOR_ONCLICK_CONTAINS_LOADVIDEOHISTORY = "a[onclick^=loadVideoHistory]";
    public static final String FIELD_VIDEO = "VIDEO";
    public static final String FIELD_INFOS = "INFOS";
    public static final String FIELD_MEDIA = "MEDIA";

    @Resource JdomService jdomService;
    @Resource HtmlService htmlService;
    @Resource ImageService imageService;
    @Resource UrlService urlService;


    public Set<Item> getItems(Podcast podcast) {
        return this.getSetItemToPodcastFromFrontTools(getRealUrl(podcast));
    }

    public String signatureOf(Podcast podcast) {
        return signatureService.generateSignatureFromURL(getRealUrl(podcast));
    }

    private String getPodcastURLOfFrontTools(String url) {
        Matcher tabs = TABS_EXTRACTOR.matcher(url);
        String liste = tabs.find() ? String.format("&liste=%d", Integer.parseInt(tabs.group(1))-1) : "";

        return htmlService
                .get(url)
                .map(p -> p.select(SELECTOR_ONCLICK_CONTAINS_LOADVIDEOHISTORY).first().attr("onclick"))
                .flatMap(s -> patternMatcher(s, ID_EXTRACTOR))
                .map(ids -> String.format(FRONT_TOOLS_URL_PATTERN, Integer.parseInt(ids.group(1)), Integer.parseInt(ids.group(2)), liste))
                .orElse("");
    }

    private Elements getHTMLListingEpisodeFromFrontTools(String canalPlusFrontToolsUrl) {
        Optional<Matcher> matcher = patternMatcher(canalPlusFrontToolsUrl, NB_PLUS_VIDEOS_PATTERN);
        return matcher
            .flatMap(id -> htmlService.get(canalPlusFrontToolsUrl))
            .map(p -> p.select("ul.features, ul.unit-gallery2"))
            .map(e -> e.get(Integer.parseInt(matcher.map(m -> m.group(1)).get())).select("li"))
            .orElse(new Elements());
    }

    private Set<Item> getSetItemToPodcastFromFrontTools(String urlFrontTools) {
        return getHTMLListingEpisodeFromFrontTools(urlFrontTools)
                .stream()
                .filter(e -> !e.hasClass("blankMS"))
                .map(e -> e.select("li._thumbs").first().id().replace("video_", ""))
                .map(Integer::valueOf)
                .map(this::getItemFromVideoId)
                .collect(toSet());
    }


    private Item getItemFromVideoId(Integer idCanalPlusVideo) {
        return urlService
                .newURL(String.format(XML_INFORMATION_PATTERN, idCanalPlusVideo))
                .flatMap(jdomService::parse)
                .map(this::itemFromXml)
                .orElse(Item.DEFAULT_ITEM);
    }

    private Item itemFromXml(Document x) {
        org.jdom2.Element infos = x.getRootElement().getChild(FIELD_VIDEO).getChild(FIELD_INFOS);
        org.jdom2.Element media = x.getRootElement().getChild(FIELD_VIDEO).getChild(FIELD_MEDIA);

        return Item.builder()
                .title(infos.getChild(FIELD_TITRAGE).getChildText(FIELD_TITRE))
                .pubdate(fromCanalPlus(infos.getChild(FIELD_PUBLICATION).getChildText(FIELD_DATE), infos.getChild(FIELD_PUBLICATION).getChildText(FIELD_HEURE)))
                .cover(imageService.getCoverFromURL(media.getChild(FIELD_IMAGES).getChildText(FIELD_GRAND)))
                .description(infos.getChild(FIELD_TITRAGE).getChildText(FIELD_SOUS_TITRE))
                .url(findUrl(media))
                .build();
    }

    private String findUrl(org.jdom2.Element media) {
        return StringUtils.isNotEmpty(media.getChild(FIELD_VIDEOS).getChildText(FIELD_QUALITY_HLS))
                ? urlService.getM3U8UrlFormMultiStreamFile(media.getChild(FIELD_VIDEOS).getChildText(FIELD_QUALITY_HLS))
                : media.getChild(FIELD_VIDEOS).getChildText(FIELD_QUALITY_HD);
    }

    private ZonedDateTime fromCanalPlus(String date, String heure) {
        LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(CANALPLUS_PATTERN));
        return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
    }

    private String getRealUrl(Podcast podcast) {
        return podcast.getUrl().contains("front_tools") ? podcast.getUrl() : this.getPodcastURLOfFrontTools(podcast.getUrl());
    }


    private Optional<Matcher> patternMatcher(String s, Pattern pattern) {
        Matcher matcher = pattern.matcher(s);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher);
    }

    @Override
    public Type type() {
        return new Type("CanalPlus", "Canal+");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "canalplus.fr") ? 1 : Integer.MAX_VALUE;
    }
}
