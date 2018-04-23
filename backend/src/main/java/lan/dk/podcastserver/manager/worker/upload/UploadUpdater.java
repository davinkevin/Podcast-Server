package lan.dk.podcastserver.manager.worker.upload;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Component("UploadUpdater")
public class UploadUpdater implements Updater {

    public static final Type TYPE = new Type("upload", "Upload");

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return HashSet.ofAll(podcast.getItems());
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
    public Type type() {
        return TYPE;
    }

    @Override
    public Integer compatibility(String url) {
        return Integer.MAX_VALUE;
    }
}
