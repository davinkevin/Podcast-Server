package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.facade.UpdateTuple;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
public class NoOpUpdater implements Updater {

    @Override
    public UpdateTuple<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast) { return null; }

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return podcast.getItems();
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return "";
    }

    @Override
    public Predicate<Item> notIn(Podcast podcast) {
        return item -> Boolean.FALSE;
    }

    @Override
    public AbstractUpdater.Type type() { return null; }

    @Override
    public Integer compatibility(String url) { return Integer.MAX_VALUE; }
}
