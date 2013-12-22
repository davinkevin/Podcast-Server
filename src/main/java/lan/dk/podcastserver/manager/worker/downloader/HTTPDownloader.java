package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

//@Scope("prototype")
@Component("HTTPDownloader")
@Scope("prototype")
public class HTTPDownloader extends AbstractDownloader {

    protected DownloadInfo info = null;

    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();
        //this.startDownload();
        //int borne = randomGenerator.nextInt(100);
        try {
            URL url = new URL(item.getUrl());
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
                            logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState());
                            break;
                        case DONE:
                            logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " - Téléchargement terminé");
                            finishDownload();
                            itemDownloadManager.removeACurrentDownload(item);
                            break;
                        case RETRYING:
                            logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState() + " " + info.getDelay());
                            break;
                        case DOWNLOADING:
                            long now = System.currentTimeMillis();
                            if (now - 1000 > last) {
                                last = now;
                                item.setProgression((int) (info.getCount()*100 / (float) info.getLength()));
                            }
                            break;
                        default:
                            break;
                    }
                }
            };


            info.extract(stopDownloading, itemSynchronisation);
            // enable multipart donwload
            //info.enableMultipart();
            // Choise target file
            //target = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + FilenameUtils.getName(String.valueOf(url)) + temporaryExtension );
            //target = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + FilenameUtils.getName(String.valueOf(url)) + temporaryExtension );
            //target.getParentFile().mkdirs();
            //logger.debug(target.getAbsolutePath() + "exist : " + target.exists());
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
        } catch (DownloadInterruptedError e) {
            logger.debug("Arrêt du téléchargement");
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //logger.debug("Download termine");
        //finishDownload();
        //itemDownloadManager.removeACurrentDownload(item);
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
