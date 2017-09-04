package lan.dk.podcastserver.manager.worker.downloader;


import io.vavr.Lazy;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static io.vavr.API.Lazy;
import static io.vavr.API.Try;

/**
 * Created by kevin on 28/02/15.
 */
@Slf4j
@Scope("prototype")
@Component("DailyMotionCloudDownloader")
public class DailyMotionCloudDownloader extends M3U8Downloader {

    private final Lazy<Option<String>> findUrl;

    public DailyMotionCloudDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
        findUrl = Lazy(this::findBestUrl);
    }

    @Override
    public String getItemUrl(Item item) {
        return findUrl.get().getOrElseThrow(() -> new RuntimeException("Url not found for " + item.getUrl()));
    }

    private Option<String> findBestUrl() {
        String realUrl = urlService.getRealURL(item.getUrl());
        return Try(() -> urlService.asStream(realUrl))
                .toOption()
                .flatMap(m3U8Service::findBestQuality)
                .map(u -> urlService.addDomainIfRelative(realUrl, u));
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("cdn.dmcloud") ? 1 : Integer.MAX_VALUE;
    }
}
