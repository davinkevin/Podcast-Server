package lan.dk.podcastserver.repository.dsl;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import lan.dk.podcastserver.entity.QItem;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kevin on 08/06/2014.
 */
public class ItemDSL {

    public static final QItem Q_ITEM = QItem.item;

    public static BooleanExpression isDownloaded(Boolean downloaded) {
        if (downloaded)
            return Q_ITEM.status.eq(Status.FINISH.value());

        return Q_ITEM.status.isNull().or(Q_ITEM.status.eq(Status.NOT_DOWNLOADED.value()));
    }

    public static BooleanExpression isNewerThan(ZonedDateTime dateTime){
        return Q_ITEM.pubdate.gt(dateTime);
    }

    public static BooleanExpression isOlderThan(ZonedDateTime dateTime){
        return Q_ITEM.pubdate.lt(dateTime);
    }

    public static BooleanExpression hasBeenDownloadedBefore(ZonedDateTime dateTime) {
        return Q_ITEM.downloadDate.lt(dateTime);
    }

    public static BooleanExpression hasBeendDownloadedAfter(ZonedDateTime dateTime) {
        return Q_ITEM.downloadDate.gt(dateTime);
    }

    public static BooleanExpression isOfType(String type) {
        return Q_ITEM.podcast.type.eq(type);
    }



    public static BooleanExpression hasToBeDeleted(Boolean deleted){
        if (deleted)
            return Q_ITEM.podcast.hasToBeDeleted.isTrue();

        return Q_ITEM.podcast.hasToBeDeleted.isFalse();

    }

    public static BooleanExpression isInId(final List<Integer> ids){
        return Q_ITEM.id.in(ids);
    }

    public static BooleanExpression isInTags(List<Tag> tags) {
        return isInTags(tags.toArray(new Tag[tags.size()]));
    }

    public static BooleanExpression isInTags(final Tag... tags) {
        return BooleanExpression.allOf(
                Arrays
                        .stream(tags)
                        .map(Q_ITEM.podcast.tags::contains)
                        .toArray(BooleanExpression[]::new)
        );
    }

    public static BooleanExpression hasStatus(final Status... statuses) {
        return BooleanExpression.anyOf(
                Arrays
                    .stream(statuses)
                    .map(status -> Q_ITEM.status.eq(status.value()))
                    .toArray(BooleanExpression[]::new)
        );
    }

    public static BooleanExpression isInPodcast(Integer podcastId) {
        return Q_ITEM.podcast.id.eq(podcastId);
    }

    public static Predicate getSearchSpecifications(List<Integer> ids, List<Tag> tags) {
        if (ids != null) {
            return isInId(ids).and(isInTags(tags) );
        }
        return isInTags(tags);
    }
}
