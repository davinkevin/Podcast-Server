package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static lan.dk.podcastserver.entity.QPlaylist.playlist;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, QueryDslPredicateExecutor<Playlist> {

    default Set<Playlist> findContainsItem(Item item) {
        return newHashSet(findAll(playlist.items.contains(item)));
    }
}
