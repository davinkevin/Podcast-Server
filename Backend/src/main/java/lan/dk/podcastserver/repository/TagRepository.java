package lan.dk.podcastserver.repository;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.QTag;
import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Created by kevin on 07/06/2014.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, UUID>, QueryDslPredicateExecutor<Tag> {

    default Option<Tag> findByNameIgnoreCase(String name) {
        return Option.of(findOne(QTag.tag.name.equalsIgnoreCase(name)));
    }

    default Set<Tag> findByNameContainsIgnoreCase(String name) {
        return HashSet.ofAll(findAll(QTag.tag.name.containsIgnoreCase(name)));
    }

}
