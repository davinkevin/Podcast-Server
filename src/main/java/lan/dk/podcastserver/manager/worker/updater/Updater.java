package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Podcast;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.Future;


public interface Updater {

    @Async("UpdateExecutor")
    public Future<Podcast> updateFeedAsync(Podcast podcast);

    public Podcast updateFeed(Podcast podcast);

    public Podcast findPodcast(String url);

    public String signaturePodcast(Podcast podcast);
}
