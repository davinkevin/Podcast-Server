package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;


@Component("WorkerService")
public class WorkerService implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected ApplicationContext context = null;
    
    @Resource UpdaterSelector updaterSelector;
    @Resource DownloaderSelector downloaderSelector;

    public Updater updaterOf(Podcast podcast) throws Exception {
        return (Updater) context.getBean(updaterSelector.of(podcast.getUrl()).getSimpleName());
    }

    public Downloader getDownloaderByType(Item item) {
        return ((Downloader) context.getBean(downloaderSelector.of(item.getUrl().toLowerCase()).getSimpleName()))
                .setItem(item);
    }

    public Finder getFinderByUrl(String url) {
        
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        
        String finderName = "RSS";
        
        if (isYoutubeUrl(url, "youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")) {
            finderName = "Youtube";
        }
            
        return context.getBean(finderName + "Finder", Finder.class);
    }

    private Boolean isYoutubeUrl(String url, String ... strings) {
        return Arrays.stream(strings)
                .anyMatch(url::contains);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
