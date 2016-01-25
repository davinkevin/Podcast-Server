package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Scope("prototype")
@Component("HTTPDownloader")
public class HTTPDownloader extends AbstractDownloader {

    @Autowired UrlService urlService;

    DownloadInfo info = null;

    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();
        //this.startDownload();
        //int borne = randomGenerator.nextInt(100);
        try {
            // initialize url information object
            info = new DownloadInfo(new URL(urlService.getRealURL(getItemUrl())));
            // extract infromation from the web
            Runnable itemSynchronisation = new HTTPWatcher(this);

            info.extract(stopDownloading, itemSynchronisation);
            target = getTagetFile(this.item);

            // create wget downloader
            WGet w = new WGet(info, target);
            // will blocks until download finishes
            w.download(stopDownloading, itemSynchronisation);
        } catch (DownloadMultipartError e) {
            e.getInfo().getParts()
                .stream()
                .map(DownloadInfo.Part::getException)
                .filter(Objects::nonNull)
                .forEach(Throwable::printStackTrace);

            stopDownload();
        } catch (DownloadInterruptedError e) {
            logger.debug("Arrêt du téléchargement");
        } catch (IOException e) {
            logger.debug("Exception during download", e);
            stopDownload();
        }
        return item;
    }

    @Slf4j
    static class HTTPWatcher implements Runnable {

        final HTTPDownloader httpDownloader;

        public HTTPWatcher(HTTPDownloader httpDownloader) {
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
                    log.debug(FilenameUtils.getName(String.valueOf(httpDownloader.getItemUrl())) + " " + info.getState());
                    break;
                case ERROR:
                    httpDownloader.stopDownload();
                    break;
                case DONE:
                    log.debug(FilenameUtils.getName(String.valueOf(httpDownloader.getItemUrl())) + " - Téléchargement terminé");
                    httpDownloader.finishDownload();
                    itemDownloadManager.removeACurrentDownload(item);
                    break;
                case RETRYING:
                    log.debug(FilenameUtils.getName(String.valueOf(httpDownloader.getItemUrl())) + " " + info.getState() + " " + info.getDelay());
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
