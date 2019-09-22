package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PodcastRepository extends JpaRepository<Podcast, UUID>{}
