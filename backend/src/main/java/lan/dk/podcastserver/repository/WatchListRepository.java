package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.WatchList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Repository
public interface WatchListRepository extends JpaRepository<WatchList, UUID> {}
