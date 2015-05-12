package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.signature.SignatureService;
import lan.dk.podcastserver.utils.facade.UpdateTuple;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Validator;
import java.util.Set;
import java.util.function.Predicate;

@Transactional(noRollbackFor=Exception.class)
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:25.0) Gecko/20100101 Firefox/25.0";

    @Resource PodcastServerParameters podcastServerParameters;
    @Resource SignatureService signatureService;
    @Resource(name="Validator") Validator validator;

    public UpdateTuple<Podcast, Set<Item>, Predicate<Item>> update(Podcast podcast) {
        try {
            logger.info("Ajout du podcast \"{}\" à l'executor", podcast.getTitle());
            String signature = signatureOf(podcast);
            if ( !StringUtils.equals(signature, podcast.getSignature()) ) {
                podcast.setSignature(signature);
                return UpdateTuple.of(podcast, getItems(podcast), notIn(podcast));
            } else {
                logger.info("Podcast non traité car signature identique : \"{}\"", podcast.getTitle());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return NO_MODIFICATION_TUPLE;
    }

    public static class Type {

        public final String key;
        public final String name;

        public Type(String key, String name) {
            this.key = key;
            this.name = name;
        }

        public String key() {
            return key;
        }

        public String name() {
            return name;
        }
    }

}
