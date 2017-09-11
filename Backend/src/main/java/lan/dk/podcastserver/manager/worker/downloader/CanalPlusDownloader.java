package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 14/08/2016
 */
@Slf4j
@Scope("prototype")
@Component("CanalPlusDownloader")
public class CanalPlusDownloader extends M3U8Downloader {

    public CanalPlusDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
    }

    @Override
    public String getFileName(Item item) {
        /* http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8 */
        String[] splitUrl = getItemUrl(item).split(",");

        int lenghtTab = splitUrl.length;
        String urlWithoutAllBandwidth = splitUrl[0] + splitUrl[lenghtTab - 2] + splitUrl[lenghtTab - 1];

        int posLastSlash = urlWithoutAllBandwidth.lastIndexOf("/");

        return FilenameUtils.getName(urlWithoutAllBandwidth.substring(0, posLastSlash).replace(".csmil", ""));
    }


    @Override
    public Integer compatibility(String url) {
        return (url.contains("canal-plus.com") || url.contains("cplus")) && url.contains("m3u8")
                ? 1
                : Integer.MAX_VALUE;
    }
}
