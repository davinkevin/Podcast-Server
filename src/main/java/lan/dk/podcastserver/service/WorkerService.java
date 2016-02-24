package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.manager.worker.selector.FinderSelector;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;


@Component("WorkerService")
public class WorkerService implements ApplicationContextAware {

    protected ApplicationContext context = null;
    
    final UpdaterSelector updaterSelector;
    final DownloaderSelector downloaderSelector;
    final FinderSelector finderSelector;
    final List<Updater> updaters;

    @Autowired
    public WorkerService(UpdaterSelector updaterSelector, DownloaderSelector downloaderSelector, FinderSelector finderSelector, List<Updater> updaters) {
        this.updaterSelector = updaterSelector;
        this.downloaderSelector = downloaderSelector;
        this.finderSelector = finderSelector;
        this.updaters = updaters;
    }

    public Set<AbstractUpdater.Type> types() {
        return updaters.stream().map(Updater::type).collect(toSet());
    }

    public Updater updaterOf(Podcast podcast) {
        return (Updater) context.getBean(updaterSelector.of(podcast.getUrl()).getSimpleName());
    }

    public Downloader downloaderOf(Item item) {
        return ((Downloader) context.getBean(downloaderSelector.of(item.getUrl().toLowerCase()).getSimpleName())).setItem(item);
    }

    public Finder finderOf(String url) {
        return ((Finder) context.getBean(finderSelector.of(url).getSimpleName()));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
