package lan.dk.podcastserver.manager.worker.noop;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.Extractor;
import lombok.extern.slf4j.Slf4j;

import static io.vavr.API.List;

/**
 * Created by kevin on 03/12/2017
 */
@Slf4j
public class NoOpExtractor implements Extractor {

    @Override
    public DownloadingItem extract(Item item) {
        return DownloadingItem.builder()
                .item(item)
                .urls(List(item.getUrl()))
                .build();
    }

    @Override
    public Integer compatibility(String url) {
        return Integer.MAX_VALUE;
    }
}
