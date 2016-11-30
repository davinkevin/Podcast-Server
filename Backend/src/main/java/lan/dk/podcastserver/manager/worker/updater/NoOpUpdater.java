package lan.dk.podcastserver.manager.worker.updater;

import javaslang.Tuple3;
import javaslang.collection.HashSet;
import javaslang.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;

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
    public AbstractUpdater.Type type() { return null; }

    @Override
    public Integer compatibility(String url) { return Integer.MAX_VALUE; }
}
