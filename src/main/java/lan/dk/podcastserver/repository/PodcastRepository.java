package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PodcastRepository  extends JpaRepository<Podcast, Integer> {

    /*@Override
    @Transactional
    @CacheEvict(value = "podcast", allEntries = true)
    <S extends Podcast> S save(S entity);

    @Override
    @Cacheable(value = "podcast", key="#root.args[0]")
    Podcast findOne(Integer integer);

    @Override
    @Cacheable(value = "podcast", key="new String('all')")
    List<Podcast> findAll();
    */

    List<Podcast> findByUrlIsNotNull();

}