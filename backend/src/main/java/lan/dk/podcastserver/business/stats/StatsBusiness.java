package lan.dk.podcastserver.business.stats;

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.dsl.ItemDSL;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Component
@RequiredArgsConstructor
public class StatsBusiness {

    private final ItemRepository itemRepository;
    private final PodcastBusiness podcastBusiness;
    private final UpdaterSelector updaterSelector;

    public List<StatsPodcastType> allStatsByTypeAndDownloadDate(Integer numberOfMonth) {
        return allStatsByType(numberOfMonth, Selector.BY_DOWNLOAD_DATE);
    }

    public List<StatsPodcastType> allStatsByTypeAndCreationDate(Integer numberOfMonth) {
        return allStatsByType(numberOfMonth, Selector.BY_CREATION_DATE);
    }

    public List<StatsPodcastType> allStatsByTypeAndPubDate(Integer numberOfMonth) {
        return allStatsByType(numberOfMonth, Selector.BY_PUBLICATION_DATE);
    }

    public Set<NumberOfItemByDateWrapper> statsByPubDate(UUID podcastId, Long numberOfMonth) {
        return statOf(podcastId, Item::getPubDate, numberOfMonth);
    }

    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(UUID id, Long numberOfMonth) {
        return statOf(id, Item::getDownloadDate, numberOfMonth);
    }

    public Set<NumberOfItemByDateWrapper> statsByCreationDate(UUID id, Long numberOfMonth) {
        return statOf(id, Item::getCreationDate, numberOfMonth);
    }

    private StatsPodcastType generateForType(Type type, Integer numberOfMonth, Selector selector) {
        ZonedDateTime dateInPast = ZonedDateTime.now().minusMonths(numberOfMonth);

        Set<NumberOfItemByDateWrapper> values =
                itemRepository
                        .findByTypeAndExpression(type, selector.filter.apply(dateInPast))
                        .toList()
                        .map(selector.getDate)
                        .filter(Objects::nonNull)
                        .map(ZonedDateTime::toLocalDate)
                        .groupBy(Function.identity())
                        .mapValues(Traversable::size)
                        .map(entry -> new NumberOfItemByDateWrapper(entry._1(), entry._2()))
                        .toSet();

        return new StatsPodcastType(type.name(), values);
    }

    private List<StatsPodcastType> allStatsByType(Integer numberOfMonth, Selector selector) {
        return updaterSelector
                .types().toList()
                .map(type -> generateForType(type, numberOfMonth, selector))
                .filter(stats -> !stats.isEmpty())
                .sorted(Comparator.comparing(StatsPodcastType::getType));
    }

    private Set<NumberOfItemByDateWrapper> statOf(UUID podcastId, Function<Item,ZonedDateTime> mapper, long numberOfMonth) {
        LocalDate dateInPast = LocalDate.now().minusMonths(numberOfMonth);
        return HashSet.ofAll(podcastBusiness.findOne(podcastId).getItems())
                .toList()
                .map(mapper)
                .filter(Objects::nonNull)
                .map(ZonedDateTime::toLocalDate)
                .filter(date -> date.isAfter(dateInPast))
                .groupBy(Function.identity())
                .mapValues(Traversable::size)
                .map(entry -> new NumberOfItemByDateWrapper(entry._1(), entry._2()))
                .toSet();
    }

    private enum Selector {

        BY_DOWNLOAD_DATE(Item::getDownloadDate, ItemDSL::hasBeenDownloadedAfter),
        BY_CREATION_DATE(Item::getCreationDate, ItemDSL::hasBeenCreatedAfter),
        BY_PUBLICATION_DATE(Item::getPubDate, ItemDSL::isNewerThan);

        Function<Item, ZonedDateTime> getDate;
        Function<ZonedDateTime, BooleanExpression> filter;

        Selector(Function<Item, ZonedDateTime> getDate, Function<ZonedDateTime, BooleanExpression> filter) {
            this.filter = filter;
            this.getDate = getDate;
        }
    }

}
