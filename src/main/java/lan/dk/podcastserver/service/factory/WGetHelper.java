package lan.dk.podcastserver.service.factory;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import lan.dk.podcastserver.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by kevin on 15/09/15 for Podcast Server
 */
@Component
public class WGetHelper {

    final UrlService urlService;

    @Autowired WGetHelper(UrlService urlService) {
        this.urlService = urlService;
    }

    public VGetParser vParser(String url) throws MalformedURLException {
        return VGet.parser(new URL(url));
    }

    public VGet vGet(VideoInfo videoInfo) {
        return new VGet(videoInfo, null);
    }

    public WGet wGet(DownloadInfo info, File targetFile) {
        return new WGet(info, targetFile);
    }

    public DownloadInfo wDownloadInfo(String url) throws MalformedURLException {
        return new DownloadInfo(new URL(url));
    }
}
