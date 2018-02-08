package lan.dk.podcastserver.manager.worker.noop;

import io.vavr.Tuple3;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;

import java.util.function.Predicate;

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
public class NoOpUpdater implements Updater {

    @Override
    public Tuple3<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast) { return Updater.NO_MODIFICATION_TUPLE; }

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return HashSet.empty();
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return null;
    }

    @Override
    public Predicate<Item> notIn(Podcast podcast) {
        return item -> Boolean.FALSE;
    }

    @Override
    public Type type() { return null; }

    @Override
    public Integer compatibility(String url) { return Integer.MAX_VALUE; }
}
