package lan.dk.podcastserver.manager.worker.updater;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.util.function.Predicate;

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Component("UploadUpdater")
public class UploadUpdater extends AbstractUpdater {

    UploadUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator) {
        super(podcastServerParameters, signatureService, validator);
    }

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
        return new Type("upload", "Upload");
    }

    @Override
    public Integer compatibility(String url) {
        return Integer.MAX_VALUE;
    }
}
