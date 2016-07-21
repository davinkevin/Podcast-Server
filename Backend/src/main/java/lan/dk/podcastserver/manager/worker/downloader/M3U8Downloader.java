package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.io.ByteStreams;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.service.M3U8Service;
import lan.dk.podcastserver.service.UrlService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

@Scope("prototype")
@Component("M3U8Downloader")
public class M3U8Downloader extends AbstractDownloader {

    @Autowired UrlService urlService;
    @Autowired M3U8Service m3U8Service;

    private List<String> urlList;
    private final M3U8Watcher watcher = new M3U8Watcher(this);

    @Override
    public Item download() {
        logger.debug("Download");

        try(BufferedReader in = readM3U8()) {
            urlList = in
                    .lines()
                    .filter(l -> !l.startsWith("#"))
                    .map(l -> urlService.urlWithDomain(getItemUrl(item), l))
                    .collect(toList());

            target = getTargetFile(item);
        } catch (IOException e) {
            logger.error("Error during fetching individual url of M3U8", e);
            stopDownload();
            return item;
        }

        try(OutputStream outputStream = Files.newOutputStream(target)) {

            for (int cpt = 0; cpt < urlList.size(); cpt++) {
                String urlFragmentToDownload = urlList.get(cpt);

                logger.debug("URL : {}", urlFragmentToDownload);
                try(InputStream is = urlService.asStream(urlFragmentToDownload)) {
                    ByteStreams.copy(is, outputStream);
                    watcher.watch(cpt);
                }

                if (Status.STOPPED == item.getStatus()) break;
            }

            if (Status.STARTED == item.getStatus()) {
                finishDownload();
                watcher.watch();
            }
        } catch (IOException e) {
            logger.error("Error during runner of M3U8Downloader", e);
            stopDownload();
        }

        return item;
    }

    @Override
    public Path getTargetFile(Item item) {

        if (nonNull(target)) return target;

        Item m3u8Item = Item.builder()
                .podcast(item.getPodcast())
                .url(urlService.getFileNameM3U8Url(getItemUrl(item)))
            .build();

        return super.getTargetFile(m3u8Item);
    }

    protected BufferedReader readM3U8() throws IOException {
        return urlService.urlAsReader(getItemUrl(item));
    }

    @Override
    public void restartDownload() {
        item.setStatus(Status.STARTED);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
        synchronized (watcher) { watcher.notify(); }
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("m3u8") ? 10 : Integer.MAX_VALUE;
    }

    @Slf4j
    @AllArgsConstructor
    static class M3U8Watcher {

        private final M3U8Downloader downloader;

        public void watch() {
            watch(null);
        }

        public void watch(Integer progress) {
            switch (downloader.item.getStatus()) {
                case STARTED:
                    broadcastProgression(progress);
                    break;
                case PAUSED:
                    log.debug("Item {} going in pause", downloader.item.getTitle());
                    synchronized (this) { Try.run(this::wait); }
                    log.debug("Item {} out of pause", downloader.item.getTitle());
                    break;
                case STOPPED:
                    log.debug("Item {} has been stopped", downloader.item.getTitle());
                    break;
                case FINISH:
                    log.debug("Item {} has been finished", downloader.item.getTitle());
                    break;
                default:
                    throw new RuntimeException("Invalid status of item "+ downloader.item.getId() +" with title " + downloader.item.getTitle());
            }
        }

        private void broadcastProgression(int cpt) {
            downloader.item.setProgression((100*cpt)/ downloader.urlList.size());
            log.debug("Progression : {}", downloader.item.getProgression());
            downloader.convertAndSaveBroadcast();
        }
    }

}
