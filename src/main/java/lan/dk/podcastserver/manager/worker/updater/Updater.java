package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;

import java.util.Set;


public interface Updater {

    public Set<Item> updateFeedAsync(Podcast podcast);

    public Podcast updateFeed(Podcast podcast);

    public Podcast findPodcast(String url);

    public String signaturePodcast(Podcast podcast);
}
