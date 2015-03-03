package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.signature.SignatureService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Validator;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

//@Component
//@Scope("prototype")
@Transactional(noRollbackFor=Exception.class)
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:25.0) Gecko/20100101 Firefox/25.0";

    @Resource PodcastServerParameters podcastServerParameters;
    @Resource SignatureService signatureService;
    @Resource(name="Validator") Validator validator;

    public Pair<Podcast, Set<Item>> update(Podcast podcast) {
        try {
            logger.info("Ajout du podcast \"{}\" à l'executor", podcast.getTitle());
            String signature = generateSignature(podcast);
            if ( !StringUtils.equals(signature, podcast.getSignature()) ) {
                podcast.setSignature(signature);
                
                Set<Item> itemNotPresentInPodcast = getItems(podcast)
                        .stream()
                        .filter(notIn(podcast))
                        .collect(toSet());
                
                return new ImmutablePair<>(podcast, itemNotPresentInPodcast);
            } else {
                logger.info("Podcast non traité car signature identique : \"{}\"", podcast.getTitle());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ImmutablePair<>(podcast, podcast.getItems());
    }
    
}
