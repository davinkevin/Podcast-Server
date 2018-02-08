package lan.dk.podcastserver.manager.worker;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import static io.vavr.API.Some;

/**
 * Created by kevin on 03/12/2017
 */
public interface Extractor {

    DownloadingItem extract(Item item);
    Integer compatibility(String url);

    default String getFileName(Item item) {
        return Some(item.getUrl())
                .map(s -> StringUtils.substringBefore(s, "?"))
                .map(FilenameUtils::getName)
                .getOrElse("");
    };
}
