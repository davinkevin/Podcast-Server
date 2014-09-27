package lan.dk.podcastserver.repository.Specification;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by kevin on 08/06/2014.
 */
public class ItemSpecifications {

    public static Specification<Item> isInTags(List<Tag> tags) {
        return isInTags(tags.toArray(new Tag[tags.size()]));
    }

    public static Specification<Item> isInTags(final Tag... tags) {
        return (root, query, cb) -> {
            Collection<Predicate> predicates = new ArrayList<>();

            Join<Item, Podcast> itemPodcastJoin = root.join("podcast");
            for(Tag tag : tags) {
                predicates.add(cb.isMember(tag, itemPodcastJoin.<List<Tag>>get("tags")));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<Item> hasId(final List<Integer> ids) {
        return (root, query, cb) -> root.<Integer>get("id").in(ids);
    }
}
