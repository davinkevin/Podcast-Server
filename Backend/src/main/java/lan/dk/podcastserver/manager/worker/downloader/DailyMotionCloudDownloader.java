package lan.dk.podcastserver.manager.worker.downloader;


import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static io.vavr.API.Try;
import static java.util.Objects.nonNull;

/**
 * Created by kevin on 28/02/15.
 */
@Slf4j
@Scope("prototype")
@Component("DailyMotionCloudDownloader")
public class DailyMotionCloudDownloader extends M3U8Downloader {

    String redirectionUrl = null;

    public DailyMotionCloudDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
    }

    @Override
    public String getItemUrl(Item item) {
        if (nonNull(redirectionUrl))
            return redirectionUrl;

        String url = urlService.getRealURL(item.getUrl());

        redirectionUrl = Try(() -> urlService.asStream(url))
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
