package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PodcastRepository  extends JpaRepository<Podcast, Integer> {

    List<Podcast> findByUrlIsNotNull();

}