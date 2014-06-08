package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by kevin on 07/06/2014.
 */
public interface TagRepository extends JpaRepository<Tag, Integer> {

    public Set<Tag> findByNameIn(Collection<String> names);
    public Tag findByName(String name);
    public List<Tag> findByNameContainsIgnoreCase(String name);
}
