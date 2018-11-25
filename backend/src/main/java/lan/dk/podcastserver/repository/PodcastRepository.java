package lan.dk.podcastserver.repository;

import com.github.davinkevin.podcastserver.entity.Podcast;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.github.davinkevin.podcastserver.entity.QPodcast.podcast;

@Repository
public interface PodcastRepository  extends JpaRepository<Podcast, UUID>, QuerydslPredicateExecutor<Podcast> {

    @CacheEvict(value = {"podcasts", "search", "stats"}, allEntries = true)
    Podcast save(Podcast p);

    @CacheEvict(value = {"podcasts", "search", "stats"}, allEntries = true)
    void deleteById(UUID id);

    default Set<Podcast> findByUrlIsNotNull() {
        return HashSet.ofAll(findAll(podcast.url.isNotNull()));
    }
}
