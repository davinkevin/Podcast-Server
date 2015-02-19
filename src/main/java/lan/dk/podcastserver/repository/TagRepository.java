package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by kevin on 07/06/2014.
 */
public interface TagRepository extends JpaRepository<Tag, Integer> {

    public List<Tag> findByNameContainsIgnoreCase(String name);
}
