package lan.dk.podcastserver.repository;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static lan.dk.podcastserver.entity.QWatchList.watchList;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Repository
public interface WatchListRepository extends JpaRepository<WatchList, UUID>, QueryDslPredicateExecutor<WatchList> {

    default Set<WatchList> findContainsItem(Item item) {
        return HashSet.ofAll(findAll(watchList.items.contains(item)));
    }
}
