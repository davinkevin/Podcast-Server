package lan.dk.podcastserver.service.health;

import lan.dk.podcastserver.manager.ItemDownloadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 19/11/2017
 */
@Component
@RequiredArgsConstructor
public class DownloaderHealthIndicator extends AbstractHealthIndicator {

    private final ItemDownloadManager itemDownloadManager;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // @formatter:off
        builder.up()
            .withDetail("isDownloading", itemDownloadManager.getNumberOfCurrentDownload() > 0)
            .withDetail("numberOfParallelDownloads", itemDownloadManager.getLimitParallelDownload())
            .withDetail("numberOfDownloading", itemDownloadManager.getNumberOfCurrentDownload())
            .withDetail("downloadingItems", itemDownloadManager.getItemsInDownloadingQueue())
            .withDetail("numberInQueue", itemDownloadManager.getWaitingQueue().length())
            .withDetail("waitingItems", itemDownloadManager.getWaitingQueue())
        .build();
        // @formatter:on
    }

}
