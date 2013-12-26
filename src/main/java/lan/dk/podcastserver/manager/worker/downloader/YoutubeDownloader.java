package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfoUser;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import lan.dk.podcastserver.entity.Item;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by kevin on 14/12/2013.
 */
@Component("YoutubeDownloader")
@Scope("prototype")
public class YoutubeDownloader extends AbstractDownloader {

    private long MAX_WAITIN_TIME = 1000*60*5;

    VideoInfo info;
    DownloadInfo downloadInfo;
    long launchDateDownload = System.currentTimeMillis();
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
            Runnable itemSynchronisation = new Runnable() {
                long last;

                @Override
                public void run() {
                    downloadInfo = info.getInfo();
                    // notify app or save download state
                    // you can extract information from DownloadInfo info;
                    switch (info.getState()) {
                        case EXTRACTING:
                        case EXTRACTING_DONE:
                            logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState());
                            break;
                        case DONE:
                            logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " - Téléchargement terminé");
                            itemDownloadManager.removeACurrentDownload(item);
                            File fileWithExtension = new File( v.getTarget().getAbsolutePath().replace(".psdownload", "") + "." + downloadInfo.getContentType().substring(downloadInfo.getContentType().lastIndexOf("/")+1) );
                            fileWithExtension.delete();
                            try {
                                FileUtils.moveFile(v.getTarget(), fileWithExtension);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            target = fileWithExtension;
                            finishDownload();
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
                                stopDownload();
                            }
                            break;
                        case DOWNLOADING:
                            int currentState = (int) (downloadInfo.getCount()*100 / (float) downloadInfo.getLength());
                            if (item.getProgression() < currentState) {
                                item.setProgression(currentState);
                                logger.debug("{} - {}%", item.getTitle(), item.getProgression());
                            }
                            break;
                        default:
                            break;
                    }
                }
            };


//            info.extract(itemSynchronisation, stopDownloading);
//            target = getTagetFile(this.item);
//            // create wget downloader
//            VGet vGet = new VGet(info, target);
//            // will blocks until download finishes
//            vGet.download(stopDownloading, itemSynchronisation);

            VideoInfoUser user = new VideoInfoUser();
            //user.setUserQuality(VideoQuality.p480);
            File targetFolder = getTagetFile(item);
            targetFolder.delete();

            //target.createNewFile();
            v = new VGet(info, targetFolder.getParentFile());

            // [OPTIONAL] call v.extract() only if you d like to get video title
            // before start download. or just skip it.
            v.extract(user, stopDownloading, itemSynchronisation);
            //System.out.println(info.getTitle());

            v.setTarget(new File(targetFolder.getParentFile(), info.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_").concat(".psdownload")));

            v.download(user, stopDownloading, itemSynchronisation);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("Download termine");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
