package lan.dk.podcastserver.manager.worker;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static io.vavr.API.*;
import static io.vavr.API.Try;

public interface
Updater {

    Logger log = org.slf4j.LoggerFactory.getLogger(Updater.class);
    Tuple3<Podcast, Set<Item>, Predicate<Item>> NO_MODIFICATION_TUPLE = Tuple(Podcast.DEFAULT_PODCAST, Set(), i -> true);

    default Tuple3<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast) {
        return Try(() -> signatureOf(podcast))
                .filter(signature -> !StringUtils.equals(signature, podcast.getSignature()))
                .andThen(podcast::setSignature)
                .map(s -> Tuple.of(podcast, getItems(podcast), notIn(podcast)))
                .onFailure(e -> logError(e, podcast))
                .getOrElse(NO_MODIFICATION_TUPLE);
    }

    default void logError(Throwable e, Podcast podcast) {
        if (NoSuchElementException.class.isInstance(e)) {
            log.info("\"{}\" hasn't change", podcast.getTitle());
            return;
        }

        log.info("\"{}\" triggered the following error during update", podcast.getTitle(), e);
    }

    Set<Item> getItems(Podcast podcast);

    String signatureOf(Podcast podcast);

    default Predicate<Item> notIn(Podcast podcast) {
        return item -> !podcast.contains(item);
    }

    Type type();

    Integer compatibility(String url);
}
