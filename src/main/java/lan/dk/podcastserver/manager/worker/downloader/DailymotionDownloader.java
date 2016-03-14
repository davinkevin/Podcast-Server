package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.service.JsonService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Scope("prototype")
@Component("DailymotionDownloader")
public class DailymotionDownloader extends HTTPDownloader {

    String url = null;

    @Autowired JsonService jsonService;

    public String getItemUrl() {

        if (nonNull(url)) {
            return url;
        }

        url = urlService
                .getPageFromURL(item.getUrl())
                .flatMap(DailymotionDownloader::getPlayerConfig)
                .flatMap(jsonService::from)
                .map(config -> ((JSONObject) ((JSONObject) config.get("metadata")).get("qualities")))
                .map(DailymotionDownloader::getMaxQuality)
                .orElse(null);

        return url;
    }

    private static String getMaxQuality(JSONObject jsonObject) {

        if (jsonObject.containsKey("720"))
            return getUrl(((JSONArray) jsonObject.get("720")));

        if (jsonObject.containsKey("480"))
            return getUrl(((JSONArray) jsonObject.get("480")));

        if (jsonObject.containsKey("380"))
            return getUrl(((JSONArray) jsonObject.get("380")));

        if (jsonObject.containsKey("240"))
            return getUrl(((JSONArray) jsonObject.get("240")));

        return getUrl(((JSONArray) jsonObject.get("auto")));
    }

    private static String getUrl(JSONArray videoItem) {
        return ((String) ((JSONObject) videoItem.get(0)).get("url"));
    }

    private static Optional<String> getPlayerConfig(String page) {
        if (!page.contains("buildPlayer({") || !page.contains("});")) return Optional.empty();

        int begin = page.indexOf("buildPlayer({");
        int end = page.indexOf("});", begin);
        return Optional.of(page.substring(begin+"buildPlayer({".length()-1, end+1));
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("dailymotion.com/video") ? 1 : Integer.MAX_VALUE;
    }

}
