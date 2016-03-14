package lan.dk.podcastserver.manager.worker.downloader;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 28/02/15.
 */
@Scope("prototype")
@Component("DailyMotionCloudDownloader")
public class DailyMotionCloudDownloader extends M3U8Downloader {

    String redirectionUrl = null;

    @Override
    public String getItemUrl() {
        if (nonNull(redirectionUrl))
            return redirectionUrl;

        Optional<String> optionalUrl = Optional.empty();
        String url = urlService.getRealURL(item.getUrl());

        try(BufferedReader in = urlService.urlAsReader(url)) {
            optionalUrl = in
                    .lines()
                    .filter(l -> l.contains("audio") && l.contains("video"))
                    .reduce((u, v) -> v);
        } catch (IOException e) {
            e.printStackTrace();
        }

        redirectionUrl = optionalUrl
                .map(u -> urlService.urlWithDomain(url, u))
                .orElse("");

        return redirectionUrl;
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("cdn.dmcloud") ? 1 : Integer.MAX_VALUE;
    }
}
