package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by kevin on 14/12/2013.
 */
@Component("YoutubeDownloader")
@Scope("prototype")
public class YoutubeDownloader extends AbstractDownloader {

    VideoInfo info;
    DownloadInfo downloadInfo;
    VGet v = null;

    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();


        //this.startDownload();
        //int borne = randomGenerator.nextInt(100);
        try {
            URL url = new URL(item.getUrl());
            // initialize url information object
            info = new VideoInfo(url);

            // extract infromation from the web
            YoutubeWatcher watcher = new YoutubeWatcher(this);

            VGetParser user = VGet.parser(url);
            info = user.info(url);

            //target.createNewFile();
            v = new VGet(info, null);

            // [OPTIONAL] call v.extract() only if you d like to get video title
            // before start download. or just skip it.
            v.extract(user, stopDownloading, watcher);
            //System.out.println(info.getTitle());

            v.setTarget(getTagetFile(item, info.getTitle()));

            v.download(user, stopDownloading, watcher);
        } catch (DownloadMultipartError e) {
            for (DownloadInfo.Part p : e.getInfo().getParts()) {
                Throwable ee = p.getException();
                if (ee != null)
                    ee.printStackTrace();
            }
            stopDownload();
        } catch (DownloadInterruptedError e) {
            logger.debug("Arrêt du téléchargement par l'interface");
            //stopDownload();
        } catch (StringIndexOutOfBoundsException | MalformedURLException e) {
            logger.error("Exception tierce : ", e);
            stopDownload();
        } catch (NullPointerException e) {
            logger.error("NullPointerException", e);
            if (itemDownloadManager.canBeReseted(item)) {
                logger.info("Reset du téléchargement Youtube {}", item.getTitle());
                itemDownloadManager.resetDownload(item);
                return null;
            }
            stopDownload();
        }
        logger.debug("Download termine");
        return item;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private File getTagetFile(Item item, String youtubleTitle) {

        if (target != null)
            return target;

       File file = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + youtubleTitle.replaceAll("[^a-zA-Z0-9.-]", "_").concat(temporaryExtension));
       if (!file.getParentFile().exists()) {
           file.getParentFile().mkdirs();
       }
       target = file;
       return target;
    }

    @Override
    public void finishDownload() {
        File fileWithExtension = new File( v.getTarget().getAbsolutePath().replace(temporaryExtension, "") + "." + downloadInfo.getContentType().substring(downloadInfo.getContentType().lastIndexOf("/")+1) );
        if (fileWithExtension.exists()) {
            fileWithExtension.delete();
        }
        try {
            FileUtils.moveFile(v.getTarget(), fileWithExtension);
        } catch (IOException e) {
            e.printStackTrace();
        }
        target = fileWithExtension;
        super.finishDownload();
    }

    static class YoutubeWatcher implements Runnable {

        protected final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final YoutubeDownloader youtubeDownloader;
        long launchDateDownload = System.currentTimeMillis();
        long MAX_WAITIN_TIME = 1000*60*5;

        public YoutubeWatcher(YoutubeDownloader youtubeDownloader) {
            this.youtubeDownloader = youtubeDownloader;
        }

        @Override
        public void run() {
            VideoInfo info = youtubeDownloader.info;
            youtubeDownloader.downloadInfo = info.getInfo();
            VGet vgetDownloader = youtubeDownloader.v;
            // notify app or save download state
            // you can extract information from DownloadInfo info;
            switch (info.getState()) {
                case EXTRACTING:
                case EXTRACTING_DONE:
                    logger.debug(FilenameUtils.getName(String.valueOf(youtubeDownloader.item.getUrl())) + " " + info.getState());
                    break;
                case ERROR:
                    youtubeDownloader.stopDownload();
                    break;
                case DONE:
                    logger.debug("{} - Téléchargement terminé", FilenameUtils.getName(vgetDownloader.getTarget().getAbsolutePath()));
                    youtubeDownloader.finishDownload();
                    break;
                case RETRYING:
                    logger.debug(info.getState() + " " + info.getDelay());
                    if (info.getDelay() == 0) {
                        logger.error(info.getException().toString());
                    }
                    if (info.getException() instanceof DownloadIOCodeError) {
                        logger.debug("Cause  : " + ((DownloadIOCodeError) info.getException()).getCode());
                    }
                    // Si le reset dure trop longtemps. Stopper.
                    if (System.currentTimeMillis() - launchDateDownload > MAX_WAITIN_TIME) {
                        youtubeDownloader.stopDownload();
                    }
                    break;
                case DOWNLOADING:
                    int currentState = (int) (youtubeDownloader.downloadInfo.getCount()*100 / (float) youtubeDownloader.downloadInfo.getLength());
                    if (youtubeDownloader.item.getProgression() < currentState) {
                        youtubeDownloader.item.setProgression(currentState);
                        logger.debug("{} - {}%", youtubeDownloader.item.getTitle(), youtubeDownloader.item.getProgression());
                        youtubeDownloader.convertAndSaveBroadcast();
                    }
                    break;
                case STOP:
                    logger.debug("Pause / Arrêt du téléchargement du téléchargement");
                    break;
                default:
                    break;
            }
        }

    }
}
