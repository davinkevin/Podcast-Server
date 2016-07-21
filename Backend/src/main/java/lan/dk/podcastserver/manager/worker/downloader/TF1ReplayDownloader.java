package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mashape.unirest.http.HttpResponse;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.UrlServiceV2;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 20/07/2016.
 */
@Scope("prototype")
@Component("TF1ReplayDownloader")
public class TF1ReplayDownloader extends M3U8Downloader {

    private static final String USER_AGENT_MOBILE = "AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)";
    private static final String APP_NAME = "sdk/Iphone/1.0";
    private static final String WAT_TIME_SERVER_URL = "http://www.wat.tv/servertime";
    private static final String SECRET = "W3m0#1mFI";
    private static final String AUTH_KEY_FORMAT = "%s-%s-%s-%s-%s";
    private static final String API_WAT_DELIVERY = "http://api.wat.tv/services/Delivery";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_FORM = "application/x-www-form-urlencoded";
    private static final String FIELD_MEDIA_ID = "mediaId";
    private static final String FIELD_APP_NAME = "appName";
    private static final String FIELD_AUTH_KEY = "authKey";
    private static final String FIELD_METHOD = "method";
    private static final String METHOD = "getUrl";
    private static final String USER_AGENT = "User-Agent";

    @Autowired HtmlService htmlService;
    @Autowired JsonService jsonService;
    @Autowired SignatureService signatureService;
    @Autowired UrlServiceV2 urlServiceV2;

    String url = null;

    @Override
    public String getItemUrl(Item item) {

        if (nonNull(this.item) && !this.item.equals(item))
            return item.getUrl();

        if (nonNull(url))
            return url;

        url = htmlService.get(item.getUrl())
                .map(d -> d.select("#zonePlayer").first())
                .map(d -> d.attr("data-src"))
                .map(src -> src.substring(src.length() - 8))
                .map(this::normalizeId)
                .map(this::getM3U8url)
                .map(this::getHighestQualityUrl)
                .orElseThrow(() -> new RuntimeException("Id not found for url " + url));

        return url;
    }

    private String normalizeId(String id) {
        if (id.matches("[0-9]+"))
            return id;

        return id.substring(1);
    }

    private String getAuthKey(String id) {
        String timestamp = Try.of(() -> urlServiceV2.get(WAT_TIME_SERVER_URL).asString())
                .map(HttpResponse::getBody)
                .map(s -> StringUtils.substringBefore(s, "|"))
                .getOrElse(StringUtils.EMPTY);

        String msg = String.format(AUTH_KEY_FORMAT, id, SECRET, APP_NAME, SECRET, timestamp);
        return signatureService.generateMD5Signature(msg) + "/" + timestamp;
    }

    private String getM3U8url(String id) {
        return Try.of(() -> urlServiceV2.post(API_WAT_DELIVERY)
                    .header(HEADER_CONTENT_TYPE, HEADER_FORM)
                    .field(FIELD_MEDIA_ID, id)
                    .field(FIELD_APP_NAME, APP_NAME)
                    .field(FIELD_AUTH_KEY, getAuthKey(id))
                    .field(FIELD_METHOD, METHOD)
                .asString())
                    .map(stringHttpResponse -> stringHttpResponse.getBody())
                    .map(jsonService::parse)
                    .map(d -> d.read("$", TF1ReplayVideoUrl.class))
                    .map(TF1ReplayVideoUrl::getMessage)
                .getOrElse("http://wat.tv/get/ipad/"+ id + ".m3u8");
    }

    private String getHighestQualityUrl(String url) {
        String realUrl = urlService.getRealURL(url, USER_AGENT_MOBILE);

        return Try.of(() -> urlServiceV2.get(url)
                .header(USER_AGENT, USER_AGENT_MOBILE)
                .asString()
        )
                .map(HttpResponse::getRawBody)
                .map(m3U8Service::findBestQuality)
                .mapTry(Optional::get)
                .map(u -> urlService.urlWithDomain(realUrl, u))
                .getOrElseThrow((e) -> new RuntimeException("Url not found for TF1 item with m3u8 url " + url, e));
    }

    @Override
    protected BufferedReader readM3U8() throws IOException {
        return Try.of(() ->
                urlServiceV2.get(getItemUrl(item))
                    .header(USER_AGENT, USER_AGENT_MOBILE)
                    .asBinary()
                    .getRawBody()
        )
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .getOrElseThrow(e -> new RuntimeException("Error during reading of m3u8 " + getItemUrl(item), e));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TF1ReplayVideoUrl {
        @Getter @Setter private String message;
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }

}
