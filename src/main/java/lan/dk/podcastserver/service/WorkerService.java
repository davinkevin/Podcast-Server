package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


@Component("WorkerService")
public class WorkerService implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected ApplicationContext context = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public Updater getUpdaterByType(Podcast podcast) throws Exception {
        Updater updater = (Updater) context.getBean(podcast.getType() + "Updater");

        if (updater == null) {
            logger.warn("No updater for the type {}", podcast.getType());
            throw new Exception("No Updater for the type " + podcast.getType());
        } else
            return updater;
    }

    public Downloader getDownloaderByType(Item item) {

        String nameDownloader = null;
        String itemUrl = item.getUrl().toLowerCase();
        if (itemUrl.contains("rtmp")) {
            nameDownloader = "RTMP";
        } else if (itemUrl.contains("www.youtube.com")) {
            nameDownloader = "Youtube";
        } else if (itemUrl.contains("www.jeuxvideo.fr")) {
            nameDownloader = "JeuxVideoFr";
        } else if (itemUrl.contains("m3u8")) {
            nameDownloader = "M3U8";
        } else if (itemUrl.contains("parleys")) {
            nameDownloader = "Parleys";
        } else if (itemUrl.contains("http")) {
            nameDownloader = "HTTP";
        }

        Downloader downloader;
        try {
            downloader = (Downloader) context.getBean(nameDownloader + "Downloader");
        } catch (BeansException e) {
            e.printStackTrace();
            return null;
        }

        if (downloader != null) {
            downloader.setItem(item);
            return downloader;
        }

        return null;
    }

    public Finder getFinderByUrl(String url) {
        
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        
        // No other implementation yet : 
        return context.getBean("RSSFinder", Finder.class);
    }
}
