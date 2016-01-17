package lan.dk.podcastserver.repository.dsl;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import lan.dk.podcastserver.entity.QItem;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Created by kevin on 08/06/2014 for Podcast Server
 */
public class ItemDSL {

    public static final QItem Q_ITEM = QItem.item;

    private ItemDSL() {
        throw new AssertionError();
    }

    public static BooleanExpression isDownloaded(Boolean downloaded) {
        if (isNull(downloaded)) return null;
        if (downloaded) return hasStatus(Status.FINISH);

        return Q_ITEM.status.isNull().or(hasStatus(Status.NOT_DOWNLOADED));
    }

    public static BooleanExpression isNewerThan(ZonedDateTime dateTime){
        return Q_ITEM.pubdate.gt(dateTime);
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
                    .map(Q_ITEM.status::eq)
                    .toArray(BooleanExpression[]::new)
        );
    }

    public static BooleanExpression isInPodcast(Integer podcastId) {
        return Q_ITEM.podcast.id.eq(podcastId);
    }

    public static Predicate getSearchSpecifications(List<Integer> ids, List<Tag> tags, Boolean downloaded) {
        return BooleanExpression.allOf(
                nonNull(ids) ? isInId(ids) : null,
                isInTags(tags),
                isDownloaded(downloaded)
        );
    }

    public static BooleanExpression isInAnyPlaylist() {
        return Q_ITEM.playlists.isNotEmpty();
    }
}
