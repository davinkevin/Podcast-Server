package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mashape.unirest.http.HttpResponse;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static io.vavr.API.*;
import static lan.dk.podcastserver.service.UrlService.USER_AGENT_DESKTOP;

/**
 * Created by kevin on 20/07/2016.
 */
@Slf4j
@Scope("prototype")
@Component("TF1ReplayDownloader")
public class TF1ReplayDownloader extends M3U8Downloader {

    private static final String WAT_WEB_HTML = "http://www.wat.tv/get/webhtml/%s";
    private static final String USER_AGENT = "User-Agent";

    private final HtmlService htmlService;
    private final JsonService jsonService;

    private final Lazy<Option<String>> _url = Lazy(this::_getItemUrl);

    public TF1ReplayDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService, HtmlService htmlService, JsonService jsonService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
        this.htmlService = htmlService;
        this.jsonService = jsonService;
    }

    @Override
    public String getFileName(Item item) {
        // https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/quotidien-deuxieme-partie-21-juin-2017.html
        return Option(item.getUrl())
                .map(s -> StringUtils.substringAfterLast(s, "/"))
                .map(s -> StringUtils.substringBeforeLast(s, "?"))
                .map(FilenameUtils::getBaseName)
                .map(s -> s + ".mp4")
                .getOrElse("");
    }

    @Override
    public String getItemUrl(Item item) {

        if (!Objects.equals(this.item, item)) {
            return super.getItemUrl(item);
        }

        return _url.get().getOrElseThrow(() -> new RuntimeException("Id not found for url " + item.getUrl()));
    }

    private Option<String> _getItemUrl() {
        return htmlService.get(item.getUrl())
                .map(d -> d.select("#zonePlayer").first())
                .map(d -> d.attr("data-src"))
                .map(src -> src.substring(src.length() - 8))
                .map(this::normalizeId)
                .map(this::getM3U8url)
                .map(this::getHighestQualityUrl);
    }

    private String normalizeId(String id) {
        if (id.matches("[0-9]+"))
            return id;

        return id.substring(1);
    }

    private String getM3U8url(String id) {
        return Try(() -> urlService.get(String.format(WAT_WEB_HTML, id))
                .header(USER_AGENT, UrlService.USER_AGENT_MOBILE)
                .asString()
        )
                .map(HttpResponse::getBody)
                .map(jsonService::parse)
                .map(JsonService.to(TF1ReplayVideoUrl.class))
                .map(TF1ReplayVideoUrl::getHls)
                .map(this::removeBitrate)
                .onFailure(e -> log.error("Error during fetching", e))
                .getOrElse("http://wat.tv/get/ipad/"+ id + ".m3u8");
    }

    private String removeBitrate(String url) {
        // Url has this format
        // http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001

        String queryParams = StringUtils.substringAfter(url, "?");

        String filteredQueryParams = List.of(queryParams.split("&"))
                .map(this::queryParamsToTuple)
                .filter(t -> !t._1().contains("bitrate"))
                .map(t -> t._1() + "=" + t._2())
                .mkString("&");

        return StringUtils.substringBefore(url, "?") + "?" + filteredQueryParams;
    }

    private Tuple2<String, String> queryParamsToTuple(String params) {
        String[] split = params.split("=", 2);
        return Tuple.of(split[0], split[1]);
    }

    private String getHighestQualityUrl(String url) {
        String realUrl = urlService.getRealURL(url, c -> c.setRequestProperty("User-Agent", USER_AGENT_DESKTOP));

        return Try(() -> urlService.get(url)
                .header(USER_AGENT, UrlService.USER_AGENT_MOBILE)
                .asString())
                .map(HttpResponse::getRawBody)
                .flatMap(is -> m3U8Service.findBestQuality(is).toTry())
                .map(u -> urlService.addDomainIfRelative(realUrl, u))
                .getOrElseThrow((e) -> new RuntimeException("Url not found for TF1 item with m3u8 url " + url, e));
    }

    @Override
    protected String withUserAgent() {
        return UrlService.USER_AGENT_MOBILE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TF1ReplayVideoUrl {
        @Getter @Setter private String hls;
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }

}
