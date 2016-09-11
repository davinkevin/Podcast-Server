package lan.dk.podcastserver.manager.worker.updater;

import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component("CanalPlusUpdater")
public class CanalPlusUpdater extends AbstractUpdater {

    private static final String CANALPLUS_PATTERN = "dd/MM/yyyy-HH:mm:ss";
    private static final String FRONT_TOOLS_URL_PATTERN = "http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=%d&ztid=%d&nbPlusVideos0=1%s";
    private static final String XML_INFORMATION_PATTERN = "http://service.canal-plus.com/video/rest/getVideos/cplus/%d";
    private static final Pattern ID_EXTRACTOR = Pattern.compile("^loadVideoHistory\\('[0-9]*','[0-9]*','[0-9]*','([0-9]*)','([0-9]*)', '[0-9]*', '[^']*'\\);");
    private static final Pattern NB_PLUS_VIDEOS_PATTERN = Pattern.compile(".*nbPlusVideos([0-9])=[1-9].*");
    private static final Pattern TABS_EXTRACTOR = Pattern.compile(".*tab=1-([0-9]*).*");
    private static final String FIELD_TITRAGE = "TITRAGE";
    private static final String FIELD_TITRE = "TITRE";
    private static final String FIELD_PUBLICATION = "PUBLICATION";
    private static final String FIELD_DATE = "DATE";
    private static final String FIELD_HEURE = "HEURE";
    private static final String FIELD_IMAGES = "IMAGES";
    private static final String FIELD_GRAND = "GRAND";
    private static final String FIELD_SOUS_TITRE = "SOUS_TITRE";
    private static final String FIELD_VIDEOS = "VIDEOS";
    private static final String FIELD_QUALITY_HLS = "HLS";
    private static final String FIELD_QUALITY_HD = "HD";
    private static final String SELECTOR_ONCLICK_CONTAINS_LOADVIDEOHISTORY = "a[onclick^=loadVideoHistory]";
    private static final String FIELD_VIDEO = "VIDEO";
    private static final String FIELD_INFOS = "INFOS";
    private static final String FIELD_MEDIA = "MEDIA";

    private final JdomService jdomService;
    private final HtmlService htmlService;
    private final ImageService imageService;
    private final M3U8Service m3U8Service;

    public CanalPlusUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, JdomService jdomService, HtmlService htmlService, ImageService imageService, M3U8Service m3U8Service) {
        super(podcastServerParameters, signatureService, validator);
        this.jdomService = jdomService;
        this.htmlService = htmlService;
        this.imageService = imageService;
        this.m3U8Service = m3U8Service;
    }

    public Set<Item> getItems(Podcast podcast) {
        return this.getSetItemToPodcastFromFrontTools(getRealUrl(podcast));
    }

    public String signatureOf(Podcast podcast) {
        return signatureService.generateSignatureFromURL(getRealUrl(podcast));
    }

    private String getPodcastURLOfFrontTools(String url) {
        Matcher tabs = TABS_EXTRACTOR.matcher(url);
        String list = tabs.find() ? String.format("&liste=%d", Integer.parseInt(tabs.group(1))-1) : "";

        return htmlService
                .get(url)
                .map(p -> p.select(SELECTOR_ONCLICK_CONTAINS_LOADVIDEOHISTORY).first().attr("onclick"))
                .flatMap(s -> patternMatcher(s, ID_EXTRACTOR))
                .map(ids -> String.format(FRONT_TOOLS_URL_PATTERN, Integer.parseInt(ids.group(1)), Integer.parseInt(ids.group(2)), list))
                .getOrElse("");
    }

    private Elements getHTMLListingEpisodeFromFrontTools(String canalPlusFrontToolsUrl) {
        Option<Matcher> matcher = patternMatcher(canalPlusFrontToolsUrl, NB_PLUS_VIDEOS_PATTERN);
        return matcher
            .flatMap(id -> htmlService.get(canalPlusFrontToolsUrl))
            .map(p -> p.select("ul.features, ul.unit-gallery2"))
            .map(e -> e.get(Integer.parseInt(matcher.map(m -> m.group(1)).get())).select("li"))
            .getOrElse(new Elements());
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
        return jdomService.parse(String.format(XML_INFORMATION_PATTERN, idCanalPlusVideo))
                    .map(this::itemFromXml)
                .getOrElse(Item.DEFAULT_ITEM);
    }

    private Item itemFromXml(Document x) {
        org.jdom2.Element infos = x.getRootElement().getChild(FIELD_VIDEO).getChild(FIELD_INFOS);
        org.jdom2.Element media = x.getRootElement().getChild(FIELD_VIDEO).getChild(FIELD_MEDIA);

        return Item.builder()
                    .title(infos.getChild(FIELD_TITRAGE).getChildText(FIELD_TITRE))
                    .pubDate(fromCanalPlus(infos.getChild(FIELD_PUBLICATION).getChildText(FIELD_DATE), infos.getChild(FIELD_PUBLICATION).getChildText(FIELD_HEURE)))
                    .cover(imageService.getCoverFromURL(media.getChild(FIELD_IMAGES).getChildText(FIELD_GRAND)))
                    .description(infos.getChild(FIELD_TITRAGE).getChildText(FIELD_SOUS_TITRE))
                    .url(findUrl(media))
                .build();
    }

    private String findUrl(org.jdom2.Element media) {
        return StringUtils.isNotEmpty(media.getChild(FIELD_VIDEOS).getChildText(FIELD_QUALITY_HLS))
                ? m3U8Service.getM3U8UrlFormMultiStreamFile(media.getChild(FIELD_VIDEOS).getChildText(FIELD_QUALITY_HLS))
                : media.getChild(FIELD_VIDEOS).getChildText(FIELD_QUALITY_HD);
    }

    private ZonedDateTime fromCanalPlus(String date, String heure) {
        LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(CANALPLUS_PATTERN));
        return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
    }

    private String getRealUrl(Podcast podcast) {
        return podcast.getUrl().contains("front_tools") ? podcast.getUrl() : this.getPodcastURLOfFrontTools(podcast.getUrl());
    }


    private Option<Matcher> patternMatcher(String s, Pattern pattern) {
        Matcher matcher = pattern.matcher(s);
        if (!matcher.find()) {
            return Option.none();
        }
        return Option.of(matcher);
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
