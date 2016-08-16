package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 14/08/2016
 */
@Scope("prototype")
@Component("CanalPlusDownloader")
public class CanalPlusDownloader extends M3U8Downloader {

    @Override
    public Path getTargetFile(Item item) {

        if (nonNull(target)) return target;

        Item canalPlusItem = Item.builder()
                .podcast(item.getPodcast())
                .url(getFileNameFromCanalPlusM3U8Url(getItemUrl(item)))
            .build();

        return super.getTargetFile(canalPlusItem);
    }

    private String getFileNameFromCanalPlusM3U8Url(String m3u8Url) {
        /* http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8 */
        String[] splitUrl = m3u8Url.split(",");

        int lenghtTab = splitUrl.length;
        String urlWithoutAllBandwith = splitUrl[0] + splitUrl[lenghtTab - 2] + splitUrl[lenghtTab - 1];

        int posLastSlash = urlWithoutAllBandwith.lastIndexOf("/");

        return FilenameUtils.getName(urlWithoutAllBandwith.substring(0, posLastSlash).replace(".csmil", ""));
    }


    @Override
    public Integer compatibility(String url) {
        return (url.contains("canal-plus.com") || url.contains("cplus")) && url.contains("m3u8")
                ? 1
                : Integer.MAX_VALUE;
    }
}
