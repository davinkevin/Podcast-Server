package lan.dk.podcastserver.repository.dsl;


import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import lan.dk.podcastserver.entity.QItem;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.*;

/**
 * Created by kevin on 08/06/2014 for Podcast Server
 */
public class ItemDSL {

    private static final QItem Q_ITEM = QItem.item;

    private ItemDSL() {
        throw new AssertionError();
    }

    public static Predicate isDownloaded(Boolean downloaded) {
        if (isNull(downloaded)) return null;
        if (downloaded) return hasStatus(Status.FINISH);

        return Q_ITEM.status.isNull().or(hasStatus(Status.NOT_DOWNLOADED));
    }

    public static BooleanExpression isNewerThan(ZonedDateTime dateTime){
        return Q_ITEM.pubDate.gt(dateTime);
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
        return Q_ITEM.id.in(ids);
    }

    private static Predicate isInTags(List<Tag> tags) {
        return isInTags(tags.toArray(new Tag[tags.size()]));
    }

    static Predicate isInTags(final Tag... tags) {
        return ExpressionUtils.allOf(Stream.of(tags)
                        .map(Q_ITEM.podcast.tags::contains)
                        .collect(toList())
                );
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

    public static Predicate getSearchSpecifications(List<UUID> ids, List<Tag> tags, Boolean downloaded) {
        return ExpressionUtils.allOf(
                nonNull(ids) ? isInId(ids) : null,
                isInTags(tags),
                nonNull(downloaded) ? downloaded ? hasStatus(Status.FINISH) : Q_ITEM.status.isNull().or(hasStatus(Status.FINISH).not()) : null
        );
    }

    public static BooleanExpression isInAnyWatchList() {
        return Q_ITEM.watchLists.isNotEmpty();
    }
}
