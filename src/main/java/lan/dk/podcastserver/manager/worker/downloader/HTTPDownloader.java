package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Scope("prototype")
@Component("HTTPDownloader")
public class HTTPDownloader extends AbstractDownloader {
    protected DownloadInfo info = null;

    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();
        //this.startDownload();
        //int borne = randomGenerator.nextInt(100);
        try {
            URL url = new URL(URLUtils.getRealURL(getItemUrl()));
            // initialize url information object
            info = new DownloadInfo(url);
            // extract infromation from the web
            Runnable itemSynchronisation = new Runnable() {
                long last;
                @Override
                public void run() {
                    // notify app or save download state
                    // you can extract information from DownloadInfo info;
                    switch (info.getState()) {
                        case EXTRACTING:
                        case EXTRACTING_DONE:
                            logger.debug(FilenameUtils.getName(String.valueOf(getItemUrl())) + " " + info.getState());
                            break;
                        case ERROR:
                            stopDownload();
                            break;
                        case DONE:
                            logger.debug(FilenameUtils.getName(String.valueOf(getItemUrl())) + " - Téléchargement terminé");
                            finishDownload();
                            itemDownloadManager.removeACurrentDownload(item);
                            break;
                        case RETRYING:
                            logger.debug(FilenameUtils.getName(String.valueOf(getItemUrl())) + " " + info.getState() + " " + info.getDelay());
                            break;
                        case DOWNLOADING:
                            long now = System.currentTimeMillis();
                            int progression = 0;
                            if (now - 1000 > last && info.getLength() != null && info.getLength() != 0L) {
                                last = now;
                                progression = (int) (info.getCount()*100 / (float) info.getLength());
                                if (item.getProgression() < progression) {
                                    item.setProgression(progression);
                                    logger.debug("Progression de {} : {}%", item.getTitle(), progression);
                                    convertAndSaveBroadcast();
                                }
                            }
                            break;
                        case STOP:
                            logger.debug("Pause / Arrêt du téléchargement du téléchargement");
                            //stopDownload();
                            break;
                        default:
                            break;
                    }
                }
            };

            info.extract(stopDownloading, itemSynchronisation);
            target = getTagetFile(this.item);

            // create wget downloader
            WGet w = new WGet(info, target);
            // will blocks until download finishes
            w.download(stopDownloading, itemSynchronisation);
        } catch (DownloadMultipartError e) {
            for (DownloadInfo.Part p : e.getInfo().getParts()) {
                Throwable ee = p.getException();
                if (ee != null)
                    ee.printStackTrace();
            }
            stopDownload();
        } catch (DownloadInterruptedError e) {
            logger.debug("Arrêt du téléchargement");
            //stopDownload();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            stopDownload();
        }
        //logger.debug("Download termine");
        //finishDownload();
        //itemDownloadManager.removeACurrentDownload(item);
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
