package lan.dk.podcastserver.manager.worker.downloader;

import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 28/02/15.
 */
@Scope("prototype")
@Component("DailyMotionCloudDownloader")
public class DailyMotionCloudDownloader extends M3U8Downloader {

    String redirectionUrl = null;

    @Override
    public String getItemUrl(Item item) {
        if (nonNull(redirectionUrl))
            return redirectionUrl;

        String url = urlService.getRealURL(item.getUrl());

        redirectionUrl = Try.of(() -> urlService.asStream(url))
                .toOption()
                .flatMap(m3U8Service::findBestQuality)
                .map(u -> urlService.addDomainIfRelative(url, u))
                .getOrElse(StringUtils.EMPTY);

        return redirectionUrl;
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("cdn.dmcloud") ? 1 : Integer.MAX_VALUE;
    }
}
