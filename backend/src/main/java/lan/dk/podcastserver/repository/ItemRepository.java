package lan.dk.podcastserver.repository;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.repository.custom.ItemRepositoryCustom;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

import static lan.dk.podcastserver.repository.dsl.ItemDSL.*;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID>, ItemRepositoryCustom, QuerydslPredicateExecutor<Item> {


    @CacheEvict(value = {"search", "stats"}, allEntries = true)
    Item save(Item item);

    @CacheEvict(value = {"search", "stats"}, allEntries = true)
    void deleteById(Item item);

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Page<Item> findByPodcast(UUID idPodcast, Pageable pageRequest) {
        return findAll(isInPodcast(idPodcast), pageRequest);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findAllNotDownloadedAndNewerThanAndNumberOfFailLessThan(ZonedDateTime date, Integer maxNumberOfRetry) {
        return HashSet.ofAll(findAll(
                isNewerThan(date).and(
                        isNotDownloaded().or(isFailedWithNotTooManyRetry(maxNumberOfRetry))
                )
        ));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findAllDownloadedAndDownloadedBeforeAndHasToBeDeleted(ZonedDateTime date) {

        Predicate downloadedAndDownloadedBeforeAndHasToBeDeleted = List.of(
                hasBeenDownloadedBefore(date), isFinished(), hasToBeDeleted(Boolean.TRUE), isInAnyWatchList().not()
        ).reduce(ExpressionUtils::and);

        return HashSet.ofAll(findAll(downloadedAndDownloadedBeforeAndHasToBeDeleted));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findByTypeAndExpression(Type type, Predicate filter) {
        return HashSet.ofAll(findAll(isOfType(type.key()).and(filter)));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findByStatus(Status... status) {
        return HashSet.ofAll(findAll(hasStatus(status)));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findAllToDownload(ZonedDateTime date, Integer numberOfFail) {
        return findAllNotDownloadedAndNewerThanAndNumberOfFailLessThan(date, numberOfFail);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    default Set<Item> findAllToDelete(ZonedDateTime date) {
        return findAllDownloadedAndDownloadedBeforeAndHasToBeDeleted(date);
    }

}
