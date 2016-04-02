package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PodcastRepository  extends JpaRepository<Podcast, UUID> {
    List<Podcast> findByUrlIsNotNull();
}