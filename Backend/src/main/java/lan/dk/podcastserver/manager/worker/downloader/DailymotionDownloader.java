package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mashape.unirest.http.HttpResponse;
import javaslang.control.Option;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Scope("prototype")
@Component("DailymotionDownloader")
public class DailymotionDownloader extends HTTPDownloader {

    String url = null;

    @Autowired JsonService jsonService;

    public String getItemUrl(Item item) {

        if (nonNull(url)) {
            return url;
        }

        url = Try.of(() -> urlService
                .get(item.getUrl())
                .asString()
        )
                .map(HttpResponse::getBody)
                .toOption()
                .flatMap(DailymotionDownloader::getPlayerConfig)
                .map(json -> jsonService.parse(json).read("metadata.qualities", DailymotionQualities.class))
                .map(DailymotionQualities::getMaxQualityUrl)
                .getOrElse(() -> null);

        return url;
    }

    private static Option<String> getPlayerConfig(String page) {
        if (!page.contains("buildPlayer({") || !page.contains("});")) return Option.none();

        int begin = page.indexOf("buildPlayer({");
        int end = page.indexOf("});", begin);
        return Option.of(page.substring(begin+"buildPlayer({".length()-1, end+1));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DailymotionQualities {
        @JsonProperty("auto") @Setter List<QualityItem> auto;
        @JsonProperty("240") @Setter List<QualityItem> p240;
        @JsonProperty("380") @Setter List<QualityItem> p380;
        @JsonProperty("480") @Setter List<QualityItem> p480;
        @JsonProperty("720") @Setter List<QualityItem> p720;
        @JsonProperty("1080") @Setter List<QualityItem> p1080;

        String getMaxQualityUrl() {
            if (nonNull(p1080) && !p1080.isEmpty())
                return p1080.get(0).getUrl();

            if (nonNull(p720) && !p720.isEmpty())
                return p720.get(0).getUrl();

            if (nonNull(p480) && !p480.isEmpty())
                return p480.get(0).getUrl();

            if (nonNull(p380) && !p380.isEmpty())
                return p380.get(0).getUrl();

            if (nonNull(p240) && !p240.isEmpty())
                return p240.get(0).getUrl();

            return auto.get(0).getUrl();
        }


        private static class QualityItem {
            @Setter @Getter private String url;
            @Setter @Getter private String type;
        }
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("dailymotion.com/video") ? 1 : Integer.MAX_VALUE;
    }

}
