package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by kevin on 07/06/2014.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    public List<Tag> findByNameContainsIgnoreCase(String name);
}
