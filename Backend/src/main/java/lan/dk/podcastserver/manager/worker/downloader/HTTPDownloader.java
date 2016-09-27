package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Scope("prototype")
@Component("HTTPDownloader")
public class HTTPDownloader extends AbstractDownloader {

    @Autowired
    UrlService urlService;
    @Autowired WGetFactory wGetFactory;

    DownloadInfo info = null;

    private final HTTPWatcher itemSynchronisation = new HTTPWatcher(this);

    @Override
    public Item download() {
        log.debug("Download");

        try {
            info = wGetFactory.newDownloadInfo(urlService.getRealURL(getItemUrl(item)));
            info.extract(stopDownloading, itemSynchronisation);
            target = getTargetFile(item);
            WGet w = wGetFactory.newWGet(info, target.toFile());
            w.download(stopDownloading, itemSynchronisation);
        } catch (DownloadMultipartError e) {
            e.getInfo().getParts()
                .stream()
                .map(DownloadInfo.Part::getException)
                .filter(Objects::nonNull)
                .forEach(Throwable::printStackTrace);

            stopDownload();
        } catch (DownloadInterruptedError e) {
            log.debug("Arrêt du téléchargement");
        } catch (IOException e) {
            log.debug("Exception during download", e);
            stopDownload();
        }
        return item;
    }

    @Override
    public Integer compatibility(String url) {
        return url.startsWith("http") ? Integer.MAX_VALUE-1 : Integer.MAX_VALUE;
    }

    @Slf4j
    static class HTTPWatcher implements Runnable {

        final HTTPDownloader httpDownloader;

        HTTPWatcher(HTTPDownloader httpDownloader) {
            this.httpDownloader = httpDownloader;
        }

        @Override
        public void run() {

            DownloadInfo info = httpDownloader.info;
            ItemDownloadManager itemDownloadManager = httpDownloader.itemDownloadManager;
            Item item = httpDownloader.item;

            switch (info.getState()) {
                case EXTRACTING:
                case EXTRACTING_DONE:
                    log.debug(FilenameUtils.getName(String.valueOf(httpDownloader.getItemUrl(item))) + " " + info.getState());
                    break;
                case ERROR:
                    httpDownloader.stopDownload();
                    break;
                case DONE:
                    log.debug(FilenameUtils.getName(String.valueOf(httpDownloader.getItemUrl(item))) + " - Téléchargement terminé");
                    httpDownloader.finishDownload();
                    itemDownloadManager.removeACurrentDownload(item);
                    break;
                case RETRYING:
                    log.debug(FilenameUtils.getName(String.valueOf(httpDownloader.getItemUrl(item))) + " " + info.getState() + " " + info.getDelay());
                    break;
                case DOWNLOADING:
                    if (isNull(info.getLength()) || (nonNull(info.getLength()) && info.getLength() == 0L)) break;

                    int progression = (int) (info.getCount()*100 / (float) info.getLength());
                    if (item.getProgression() < progression) {
                        item.setProgression(progression);
                        log.debug("Progression de {} : {}%", item.getTitle(), progression);
                        httpDownloader.convertAndSaveBroadcast();
                    }
                    break;
                case STOP:
                    log.debug("Pause / Arrêt du téléchargement du téléchargement");
                    break;
                default:
                    break;
            }
        }
    }
}
