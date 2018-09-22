package lan.dk.podcastserver.manager.worker.sixplay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import io.vavr.Value;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.parser.SixPlayParsingException;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.service.JsonService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static io.vavr.API.Option;
import static io.vavr.API.Try;
import static java.util.Objects.nonNull;

/**
 * Created by kevin on 20/12/2016 for Podcast Server
 */
@Slf4j
@Component("SixPlayUpdater")
@RequiredArgsConstructor
public class SixPlayUpdater implements Updater {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private static final TypeRef<Set<SixPlayItem>> TYPE_ITEMS = new TypeRef<Set<SixPlayItem>>(){};
    private static final TypeRef<HashMap<String, Object>> TYPE_KEYS = new TypeRef<HashMap<String, Object>>(){};

    private static final String URL_TEMPLATE_PODCAST = "http://www.6play.fr/%s-p_%d/";
    private static final String PROGRAM_CODE_SELECTOR = "program.programsById.%d.code";
    private static final String PROGRAM_ID_SELECTOR = "program.programsById";
    private static final String VIDEO_BY_ID_SELECTOR = "video.programVideoById[*]";

    private final SignatureService signatureService;
    private final HtmlService htmlService;
    private final JsonService jsonService;
    private final ImageService imageService;

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return Try(() ->
                htmlService.get(podcast.getUrl())
                        .map(d -> this.extractItems(d.select("script")))
                        .getOrElse(HashSet::empty)
        ).getOrElseThrow(SixPlayParsingException::new);
    }

    private Set<Item> extractItems(Elements script) {
        Option<DocumentContext> root6Play = extractJson(script);

        Integer programId = root6Play
                .map(JsonService.to(PROGRAM_ID_SELECTOR, TYPE_KEYS))
                .map(HashMap::keySet)
                .flatMap(Value::toOption)
                .map(Integer::valueOf)
                .getOrElseThrow(() -> new RuntimeException("programId not found in react store"));

        String programCode = root6Play
                .map(JsonService.to(String.format(PROGRAM_CODE_SELECTOR, programId), String.class))
                .getOrElseThrow(() -> new RuntimeException("programCode not found in react store"));

        String basePath = String.format(URL_TEMPLATE_PODCAST, programCode, programId);

        return root6Play
                .map(JsonService.to(VIDEO_BY_ID_SELECTOR, TYPE_ITEMS))
                .getOrElse(HashSet::empty)
                .map(s -> this.convertToItem(s, basePath));
    }

    private Item convertToItem(SixPlayItem i, String basePath) {
        return Item.builder()
                .title(i.getTitle())
                .pubDate(i.getLastDiffusion())
                .length(i.getDuration())
                .url(i.url(basePath))
                .description(i.getDescription())
                .cover(imageService.getCoverFromURL(i.cover()))
                .build();
    }

    private Option<DocumentContext> extractJson(Elements elements) {
        return getRoot6Play(elements)
                .map(this::removeJS)
                .map(jsonService::parse);
    }

    private String removeJS(String s) {
        return Option(s)
                .map(String::trim)
                .map(c -> c.replaceAll("function [^}]*", "{}"))
                .map(c -> StringUtils.removeEnd(c, ";"))
                .getOrElse(s);
    }

    public static Option<String> getRoot6Play(Elements elements) {
        return HashSet.ofAll(elements)
                .find(s -> s.html().contains("root."))
                .map(Element::html)
                .map(s -> StringUtils.substringBetween(s, " = ", "}(this));"));
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return Try(() ->
                htmlService.get(podcast.getUrl())
                        .map(d -> d.select("script"))
                        .flatMap(this::extractJson)
                        .map(JsonService.extract("video.programVideosBySubCategory"))
                        .map(Object::toString)
                        .map(signatureService::fromText)
                        .getOrElseThrow(() -> new RuntimeException("Error during signature of podcast " + podcast.getTitle()))
        ).getOrElseThrow(SixPlayParsingException::new);
    }

    @Override
    public Type type() {
        return new Type("SixPlay", "6Play");
    }

    @Override
    public Integer compatibility(String url) {
        return isFrom6Play(url);
    }

    public static int isFrom6Play(String url) {
        return nonNull(url) && url.contains("www.6play.fr/") ? 1 : Integer.MAX_VALUE;
    }

    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SixPlayItem{

        @Getter @Setter Image display_image;
        @Getter @Setter String code;
        @Getter @Setter String description;
        @Getter @Setter String title;
        @Setter String lastDiffusion; /* 2016-12-18 11:20:00 */
        @Getter @Setter Long duration;
        @Getter @Setter String id;

        ZonedDateTime getLastDiffusion() {
            return Option(lastDiffusion)
                    .map(l -> ZonedDateTime.of(LocalDateTime.parse(l, DATE_FORMATTER), ZoneId.of("Europe/Paris")))
                    .getOrElse(ZonedDateTime::now);
        }

        String url(String basePath) {
            return basePath + code + this.shortId();
        }

        String cover() {
            return Option(display_image)
                    .map(Image::url)
                    .getOrElse(() -> null);
        }

        private String shortId() {
            return Option(id)
                    .map(s -> s.substring(0, 1))
                    .map(v -> "-" + v + "_")
                    .map(v -> v + StringUtils.substringAfter(id, "_"))
                    .getOrElse("");
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Image {

            private static final String domain = "https://images.6play.fr";
            private static final String path = "/v1/images/%s/raw?width=600&height=336&fit=max&quality=60&format=jpeg&interlace=1";
            private static final String salt = "54b55408a530954b553ff79e98";

            @Getter @Setter Integer external_key;

            public String url() {
                String path = String.format(Image.path, external_key);
                return domain + path + "&hash=" + DigestUtils.sha1Hex(path + salt);
            }
        }
    }
}
