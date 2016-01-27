package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.factory.WGetHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.ZonedDateTime.*;

/**
 * Created by kevin on 14/12/2013 for Podcast Server
 */
@Scope("prototype")
@Component("YoutubeDownloader")
public class YoutubeDownloader extends AbstractDownloader {

    VideoInfo info;
    DownloadInfo downloadInfo;
    VGet v = null;

    @Autowired WGetHelper wGetHelper;

    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();

        YoutubeWatcher watcher = new YoutubeWatcher(this);
        try {
            // extract infromation from the web
            VGetParser parser = wGetHelper.vParser(item.getUrl());
            info = parser.info(new URL(item.getUrl()));
            v = wGetHelper.vGet(info);

            // [OPTIONAL] call v.extract() only if you d like to get video title
            // before start download. or just skip it.
            v.extract(parser, stopDownloading, watcher);
            v.setTarget(getTagetFile(item, info.getTitle()));
            v.download(parser, stopDownloading, watcher);
        } catch (DownloadMultipartError e) {
            e.getInfo()
                .getParts()
                .stream()
                .map(DownloadInfo.Part::getException)
                .filter(Objects::nonNull)
                .forEach(Throwable::printStackTrace);

            stopDownload();
        } catch (DownloadInterruptedError e) {
            logger.debug("Arrêt du téléchargement par l'interface");
        } catch (StringIndexOutOfBoundsException | MalformedURLException | NullPointerException e) {
            logger.error("Exception tierce : ", e);
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

    @Slf4j
    static class YoutubeWatcher implements Runnable {

        private final YoutubeDownloader youtubeDownloader;
        private final ZonedDateTime launchDateDownload = now();
        Integer MAX_WAITING_MINUTE = 5;

        public YoutubeWatcher(YoutubeDownloader youtubeDownloader) {
            this.youtubeDownloader = youtubeDownloader;
        }

        @Override
        public void run() {
            VideoInfo info = youtubeDownloader.info;
            DownloadInfo downloadInfo = attachDownloadInfo(info);
            VGet vgetDownloader = youtubeDownloader.v;
            Item item = youtubeDownloader.item;
            // notify app or save download state
            // you can extract information from DownloadInfo info;
            switch (info.getState()) {
                case EXTRACTING:
                case EXTRACTING_DONE:
                    log.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState());
                    break;
                case ERROR:
                    youtubeDownloader.stopDownload();
                    break;
                case DONE:
                    log.debug("{} - Téléchargement terminé", FilenameUtils.getName(vgetDownloader.getTarget().getAbsolutePath()));
                    youtubeDownloader.finishDownload();
                    break;
                case RETRYING:
                    log.debug(info.getState() + " " + info.getDelay());
                    if (info.getDelay() == 0) {
                        log.error(info.getException().toString());
                    }
                    if (DownloadIOCodeError.class.isInstance(info.getException())) {
                        log.debug("Cause  : " + DownloadIOCodeError.class.cast(info.getException()).getCode());
                    }
                    // Si le reset dure trop longtemps. Stopper.
                    if (launchDateDownload.isBefore(now().minusMinutes(MAX_WAITING_MINUTE))) {
                        youtubeDownloader.stopDownload();
                    }
                    break;
                case DOWNLOADING:
                    int currentState = (int) (downloadInfo.getCount()*100 / (float) downloadInfo.getLength());
                    if (item.getProgression() < currentState) {
                        item.setProgression(currentState);
                        log.debug("{} - {}%", item.getTitle(), item.getProgression());
                        youtubeDownloader.convertAndSaveBroadcast();
                    }
                    break;
                case STOP:
                    log.debug("Pause / Arrêt du téléchargement du téléchargement");
                    break;
                default:
                    break;
            }
        }

        private DownloadInfo attachDownloadInfo(VideoInfo info) {
            youtubeDownloader.downloadInfo = info.getInfo();
            return youtubeDownloader.downloadInfo;
        }

    }
}
