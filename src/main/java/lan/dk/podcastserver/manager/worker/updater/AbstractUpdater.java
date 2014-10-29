package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Validator;
import java.util.Set;

//@Component
//@Scope("prototype")
@Transactional(noRollbackFor=Exception.class)
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:25.0) Gecko/20100101 Firefox/25.0";

    @Value("${serverURL:http://localhost:8080}") private String serverURL;
    @Resource(name="Validator") Validator validator;

    public Pair<Podcast, Set<Item>> update(Podcast podcast) {
        try {
            logger.info("Ajout du podcast \"{}\" à l'executor", podcast.getTitle());
            String signature = generateSignature(podcast);
            if ( !StringUtils.equals(signature, podcast.getSignature()) ) {
                podcast.setSignature(signature);


                return new ImmutablePair<>(podcast, getItems(podcast));
            } else {
                logger.info("Podcast non traité car signature identique : \"{}\"", podcast.getTitle());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ImmutablePair<>(podcast, podcast.getItems());
    }
}
