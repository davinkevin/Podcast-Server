package lan.dk.podcastserver.business.stats;

import com.querydsl.core.types.dsl.BooleanExpression;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.dsl.ItemDSL;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

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

    private StatsPodcastType generateForType(AbstractUpdater.Type type, Integer numberOfMonth, Selector selector) {
        ZonedDateTime dateInPast = ZonedDateTime.now().minusMonths(numberOfMonth);

        Set<NumberOfItemByDateWrapper> values =
                itemRepository
                        .findByTypeAndExpression(type, selector.filter.apply(dateInPast))
                        .toJavaStream()
                        .map(selector.getDownloadDate)
                        .filter(Objects::nonNull)
                        .map(ZonedDateTime::toLocalDate)
                        .collect(groupingBy(o -> o, counting()))
                        .entrySet()
                        .stream()
                        .map(entry -> new NumberOfItemByDateWrapper(entry.getKey(), entry.getValue()))
                        .collect(toSet());

        return new StatsPodcastType(type.name(), values);
    }

    private List<StatsPodcastType> allStatsByType(Integer numberOfMonth, Selector selector) {
        return updaterSelector
                .types()
                .stream()
                .map(type -> generateForType(type, numberOfMonth, selector))
                .filter(stats -> stats.values().size() > 0)
                .sorted(Comparator.comparing(StatsPodcastType::type))
                .collect(toList());
    }

    private Set<NumberOfItemByDateWrapper> statOf(UUID podcastId, Function<Item,ZonedDateTime> mapper, long numberOfMonth) {
        LocalDate dateInPast = LocalDate.now().minusMonths(numberOfMonth);
        return podcastBusiness.findOne(podcastId)
                .getItems()
                .stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .map(ZonedDateTime::toLocalDate)
                .filter(date -> date.isAfter(dateInPast))
                .collect(groupingBy(o -> o, counting()))
                .entrySet()
                .stream()
                .map(entry -> new NumberOfItemByDateWrapper(entry.getKey(), entry.getValue()))
                .collect(toSet());
    }

    private enum Selector {

        BY_DOWNLOAD_DATE(Item::getDownloadDate, ItemDSL::hasBeenDownloadedAfter),
        BY_CREATION_DATE(Item::getCreationDate, ItemDSL::hasBeenCreatedAfter),
        BY_PUBLICATION_DATE(Item::getPubDate, ItemDSL::isNewerThan);

        Function<Item, ZonedDateTime> getDownloadDate;
        Function<ZonedDateTime, BooleanExpression> filter;

        Selector(Function<Item, ZonedDateTime> getDownloadDate, Function<ZonedDateTime, BooleanExpression> filter) {
            this.filter = filter;
            this.getDownloadDate = getDownloadDate;
        }
    }

}
