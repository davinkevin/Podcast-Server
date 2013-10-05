package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverRepository extends JpaRepository<Item, Integer> {

    public List<Cover> findByURLike(String URL);

}
