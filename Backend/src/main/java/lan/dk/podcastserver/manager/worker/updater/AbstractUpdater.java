package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonProperty;
import javaslang.Tuple;
import javaslang.Tuple3;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Validator;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Transactional(noRollbackFor=Exception.class)
public abstract class AbstractUpdater implements Updater {

    @Resource PodcastServerParameters podcastServerParameters;
    @Resource SignatureService signatureService;
    @Resource(name="Validator") Validator validator;

    public Tuple3<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast) {
        try {
            log.info("Ajout du podcast \"{}\" à l'executor", podcast.getTitle());
            String signature = signatureOf(podcast);
            if ( !StringUtils.equals(signature, podcast.getSignature()) ) {
                podcast.setSignature(signature);
                return Tuple.of(podcast, getItems(podcast), notIn(podcast));
            } else {
                log.info("Podcast non traité car signature identique : \"{}\"", podcast.getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return NO_MODIFICATION_TUPLE;
    }

    public static class Type {

        private final String key;
        private final String name;

        public Type(String key, String name) {
            this.key = key;
            this.name = name;
        }

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
