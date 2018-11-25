package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.WatchList;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.github.davinkevin.podcastserver.entity.QWatchList.watchList;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Repository
public interface WatchListRepository extends JpaRepository<WatchList, UUID>, QuerydslPredicateExecutor<WatchList> {

    default Set<WatchList> findContainsItem(Item item) {
        return HashSet.ofAll(findAll(watchList.items.contains(item)));
    }
}
