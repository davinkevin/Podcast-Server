package lan.dk.podcastserver.repository;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.QPodcast;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PodcastRepository  extends JpaRepository<Podcast, UUID>, QueryDslPredicateExecutor<Podcast> {

    @CacheEvict(value = {"podcasts", "search", "stats"}, allEntries = true)
    Podcast save(Podcast p);

    @CacheEvict(value = {"podcasts", "search", "stats"}, allEntries = true)
    void delete(UUID id);

    default Set<Podcast> findByUrlIsNotNull() {
        return HashSet.ofAll(findAll(QPodcast.podcast.url.isNotNull()));
    }
}
