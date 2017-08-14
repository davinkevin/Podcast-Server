package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import static io.vavr.API.Try;

@Slf4j
@Transactional(noRollbackFor=Exception.class)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractUpdater implements Updater {

    final PodcastServerParameters podcastServerParameters;
    final SignatureService signatureService;
    final Validator validator;

    public Tuple3<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast) {
        log.info("\"{}\" added to executor", podcast.getTitle());

        return Try(() -> signatureOf(podcast))
                .filter(signature -> !StringUtils.equals(signature, podcast.getSignature()))
                .andThen(podcast::setSignature)
                .map(s -> Tuple.of(podcast, getItems(podcast), notIn(podcast)))
                .onFailure(e -> logError(e, podcast))
                .getOrElse(NO_MODIFICATION_TUPLE);
    }

    private void logError(Throwable e, Podcast podcast) {
        if (NoSuchElementException.class.isInstance(e)) {
            log.info("\"{}\" hasn't change", podcast.getTitle());
            return;
        }

        log.info("\"{}\" triggered the following error during update", podcast.getTitle(), e);
    }

    @RequiredArgsConstructor
    public static class Type {

        private final String key;
        private final String name;

        @JsonProperty("key")
        public String key() {
            return key;
        }

        @JsonProperty("name")
        public String name() {
            return name;
        }
    }

}
