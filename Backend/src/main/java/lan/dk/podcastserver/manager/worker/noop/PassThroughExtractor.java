package lan.dk.podcastserver.manager.worker.noop;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static io.vavr.API.List;
import static java.lang.Integer.MAX_VALUE;

/**
 * Created by kevin on 03/12/2017
 */
@Slf4j
@Component
@Scope("prototype")
public class PassThroughExtractor implements Extractor {
    
    @Override
    public DownloadingItem extract(Item item) {
        return DownloadingItem.builder()
                .item(item)
                .urls(List(item.getUrl()))
                .build();
    }

    @Override
    public Integer compatibility(String url) {
        return MAX_VALUE-1;
    }

}
