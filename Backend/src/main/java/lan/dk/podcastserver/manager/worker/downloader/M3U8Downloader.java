package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.io.ByteStreams;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

@Scope("prototype")
@Component("M3U8Downloader")
public class M3U8Downloader extends AbstractDownloader {

    @Autowired UrlService urlService;

    List<String> urlList;
    final M3U8Watcher watcher = new M3U8Watcher(this);

    @Override
    public Item download() {
        logger.debug("Download");

        if (watcher.hasBeenStarted().get()) {
            synchronized (watcher) {
                watcher.notify();
            }
            return item;
        }

        try(BufferedReader in = urlService.urlAsReader(getItemUrl(item))) {
            urlList = in
                    .lines()
                    .filter(l -> !l.startsWith("#"))
                    .map(l -> urlService.urlWithDomain(getItemUrl(item), l))
                    .collect(toList());

            target = getTagetFile(item);

            watcher.run();
        } catch (IOException e) {
            logger.error("Error during fetching individual url of M3U8", e);
            stopDownload();
        }

        return item;
    }

    @Override
    public File getTagetFile (Item item) {

        if (nonNull(target)) return target;

        Item m3u8Item = Item.builder()
                    .podcast(item.getPodcast())
                    .url(urlService.getFileNameM3U8Url(getItemUrl(item)))
                .build();

        return super.getTagetFile(m3u8Item);
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("m3u8") ? 10 : Integer.MAX_VALUE;
    }

    @Slf4j
    static class M3U8Watcher implements Runnable {

        private final M3U8Downloader downloader;;
        private List<String> urlList;
        private UrlService urlService;
        private Item item;

        private AtomicBoolean hasBeenStarted = new AtomicBoolean(false);

        M3U8Watcher(M3U8Downloader downloader) {
            this.downloader = downloader;
        }

        @Override
        public void run() {
            start();
            item = downloader.item;
            urlList = downloader.urlList;
            urlService = downloader.urlService;

            try(OutputStream outputStream = new FileOutputStream(downloader.target)) {

                for (int cpt = 0; cpt < urlList.size(); cpt++) {
                    String urlFragmentToDownload = urlList.get(cpt);

                    log.debug("URL : {}", urlFragmentToDownload);
                    try(InputStream is = urlService.asStream(urlFragmentToDownload)) {
                        ByteStreams.copy(is, outputStream);
                        broadcastProgression(cpt);
                    }

                    if (downloader.stopDownloading.get()) {
                        if (Status.STOPPED == item.getStatus()) break;

                        if (Status.PAUSED == item.getStatus()) {
                            synchronized (this) {
                                wait();
                                item.setStatus(Status.STARTED);
                            }
                        }
                    }
                }

                if (Status.STARTED == item.getStatus()) {
                    downloader.finishDownload();
                }
            } catch (IOException | InterruptedException e) {
                log.error("Error during runner of M3U8Downloader", e);
                downloader.stopDownload();
            }
        }

        private void broadcastProgression(int cpt) {
            item.setProgression((100*cpt)/ urlList.size());
            log.debug("Progression : {}", item.getProgression());
            downloader.convertAndSaveBroadcast();
        }

        AtomicBoolean hasBeenStarted() {
            return hasBeenStarted;
        }

        private void start() {
            hasBeenStarted.set(true);
        }
    }

}
