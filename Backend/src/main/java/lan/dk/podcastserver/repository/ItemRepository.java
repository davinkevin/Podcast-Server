package lan.dk.podcastserver.repository;

import com.google.common.collect.Sets;
import com.mysema.query.types.expr.BooleanExpression;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.repository.custom.ItemRepositoryCustom;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static com.mysema.query.types.expr.BooleanExpression.allOf;
import static lan.dk.podcastserver.repository.dsl.ItemDSL.*;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID>, ItemRepositoryCustom, QueryDslPredicateExecutor<Item> {

    @CacheEvict(value = {"search", "stats"}, allEntries = true)
    Item save(Item item);

    @CacheEvict(value = {"search", "stats"}, allEntries = true)
    void delete(Item item);

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Page<Item> findByPodcast(UUID idPodcast, PageRequest pageRequest) {
        return findAll(isInPodcast(idPodcast), pageRequest);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Iterable<Item> findAllToDownload(ZonedDateTime date) {
        return findAllNotDownloadedAndNewerThan(date);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Iterable<Item> findAllNotDownloadedAndNewerThan(ZonedDateTime date) {
        return findAll(isNewerThan(date).and(isDownloaded(Boolean.FALSE)));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findAllDownloadedAndDownloadedBeforeAndHasToBeDeleted(ZonedDateTime date) {
        return Sets.newHashSet(findAll(allOf(hasBeenDownloadedBefore(date), isDownloaded(Boolean.TRUE), hasToBeDeleted(Boolean.TRUE), isInAnyWatchList().not())));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findAllToDelete(ZonedDateTime date) {
        return findAllDownloadedAndDownloadedBeforeAndHasToBeDeleted(date);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findByTypeAndExpression(AbstractUpdater.Type type, BooleanExpression filter) {
        return Sets.newHashSet(findAll(isOfType(type.key()).and(filter)));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Iterable<Item> findByStatus(Status... status) {
        return findAll(hasStatus(status));
    }

}
