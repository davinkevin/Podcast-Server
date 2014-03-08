package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Cover;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverRepository extends JpaRepository<Cover, Integer> {

}
