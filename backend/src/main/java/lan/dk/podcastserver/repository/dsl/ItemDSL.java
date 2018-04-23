package lan.dk.podcastserver.repository.dsl;


import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.QItem;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static io.vavr.API.Option;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

/**
 * Created by kevin on 08/06/2014 for Podcast Server
 */
public class ItemDSL {

    private static final QItem Q_ITEM = QItem.item;

    private ItemDSL() {
        throw new AssertionError();
    }

    public static Predicate isFinished() {
        return hasStatus(Status.FINISH);
    }

    public static BooleanExpression isNotDownloaded() {
        return Q_ITEM.status.isNull().or(hasStatus(Status.NOT_DOWNLOADED));
    }

    public static BooleanExpression isNewerThan(ZonedDateTime dateTime){
        return Q_ITEM.pubDate.gt(dateTime);
    }

    public static BooleanExpression isFailedWithNotTooManyRetry(Integer maxNumberOfFail) {
        return Q_ITEM.numberOfFail.lt(maxNumberOfFail).and(hasStatus(Status.FAILED));
    }

    public static BooleanExpression hasBeenDownloadedBefore(ZonedDateTime dateTime) {
        return Q_ITEM.downloadDate.lt(dateTime);
    }

    public static BooleanExpression hasBeenDownloadedAfter(ZonedDateTime dateTime) { return Q_ITEM.downloadDate.gt(dateTime); }
    public static BooleanExpression hasBeenCreatedAfter(ZonedDateTime dateTime) { return Q_ITEM.creationDate.gt(dateTime); }

    public static BooleanExpression isOfType(String type) {
        return Q_ITEM.podcast.type.eq(type);
    }



    public static BooleanExpression hasToBeDeleted(Boolean deleted){
        if (deleted)
            return Q_ITEM.podcast.hasToBeDeleted.isTrue();

        return Q_ITEM.podcast.hasToBeDeleted.isFalse();

    }

    static BooleanExpression isInId(final List<UUID> ids){
        return Q_ITEM.id.in(ids.toJavaList());
    }

    private static Predicate isInTags(Set<Tag> tags) {
        return ExpressionUtils.allOf(tags
                .map(Q_ITEM.podcast.tags::contains)
                .map(Predicate.class::cast)
                .toJavaList());
    }

    public static Predicate hasStatus(final Status... statuses) {
        return ExpressionUtils.anyOf(Stream.of(statuses)
                        .map(Q_ITEM.status::eq)
                        .collect(toList())
                );
    }

    public static BooleanExpression isInPodcast(UUID podcastId) {
        return Q_ITEM.podcast.id.eq(podcastId);
    }

    public static Predicate getSearchSpecifications(List<UUID> ids, Set<Tag> tags, Set<Status> statuses) {
        return ExpressionUtils.allOf(
                Option(ids).map(ItemDSL::isInId).getOrElse(() -> null),
                isInTags(tags),
                hasStatus(statuses.toJavaArray(Status.class))
        );
    }

    public static BooleanExpression isInAnyWatchList() {
        return Q_ITEM.watchLists.isNotEmpty();
    }
}
