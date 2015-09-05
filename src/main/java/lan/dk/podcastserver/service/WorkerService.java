package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;


@Component("WorkerService")
public class WorkerService implements ApplicationContextAware {

    protected ApplicationContext context = null;
    
    final UpdaterSelector updaterSelector;
    final DownloaderSelector downloaderSelector;
    final List<Updater> updaters;

    @Autowired
    public WorkerService(UpdaterSelector updaterSelector, DownloaderSelector downloaderSelector, List<Updater> updaters) {
        this.updaterSelector = updaterSelector;
        this.downloaderSelector = downloaderSelector;
        this.updaters = updaters;
    }

    public Set<AbstractUpdater.Type> types() {
        return updaters
                .stream()
                .map(Updater::type)
                .collect(toSet());
    }

    public Updater updaterOf(Podcast podcast) {
        return (Updater) context.getBean(updaterSelector.of(podcast.getUrl()).getSimpleName());
    }

    public Downloader getDownloaderByType(Item item) {
        return ((Downloader) context.getBean(downloaderSelector.of(item.getUrl().toLowerCase()).getSimpleName()))
                .setItem(item);
    }

    public Finder finderOf(String url) {
        
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
