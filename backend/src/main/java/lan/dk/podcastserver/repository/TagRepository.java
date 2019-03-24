package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Tag;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.github.davinkevin.podcastserver.entity.QTag.tag;

/**
 * Created by kevin on 07/06/2014.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, UUID>, QuerydslPredicateExecutor<Tag> {

    default Option<Tag> findByNameIgnoreCase(String name) {
        return Option.ofOptional(findOne(tag.name.equalsIgnoreCase(name)));
    }

}
