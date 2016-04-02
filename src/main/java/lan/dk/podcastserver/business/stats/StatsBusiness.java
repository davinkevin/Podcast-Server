package lan.dk.podcastserver.business.stats;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StatsBusiness {

    final ItemRepository itemRepository;
    final PodcastBusiness podcastBusiness;
    final UpdaterSelector updaterSelector;

    private StatsPodcastType generateForType(AbstractUpdater.Type type, Integer numberOfMonth) {
        ZonedDateTime dateInPast = ZonedDateTime.now().minusMonths(numberOfMonth);

        Set<NumberOfItemByDateWrapper> values =
                ((List<Item>) itemRepository.findByTypeAndDownloadDateAfter(type, dateInPast))
                .stream()
                .map(Item::getDownloadDate)
                .filter(Objects::nonNull)
                .map(ZonedDateTime::toLocalDate)
                .collect(groupingBy(o -> o, counting()))
                .entrySet()
                .stream()
                .map(entry -> new NumberOfItemByDateWrapper(entry.getKey(), entry.getValue()))
                .collect(toSet());

        return new StatsPodcastType(type.name(), values);
    }

    public List<StatsPodcastType> allStatsByType(Integer numberOfMonth) {
        return updaterSelector
                .types()
                .stream()
                .map(type -> generateForType(type, numberOfMonth))
                .filter(stats -> stats.values().size() > 0)
                .sorted((s1, s2) -> s1.type().compareTo(s2.type()))
                .collect(toList());
    }

    public Set<NumberOfItemByDateWrapper> statByPubDate(UUID podcastId, Long numberOfMonth) {
        return statOf(podcastId, Item::getPubdate, numberOfMonth);
    }

    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(UUID id, Long numberOfMonth) {
        return statOf(id, Item::getDownloadDate, numberOfMonth);
    }

    @SuppressWarnings("unchecked")
    private Set<NumberOfItemByDateWrapper> statOf(UUID podcastId, Function<? extends Item, ? extends ZonedDateTime> mapper, long numberOfMonth) {
        LocalDate dateInPast = LocalDate.now().minusMonths(numberOfMonth);
        return podcastBusiness.findOne(podcastId)
                .getItems()
                .stream()
                .map((Function<? super Item, ? extends ZonedDateTime>) mapper)
                .filter(date -> date != null)
                .map(ZonedDateTime::toLocalDate)
                .filter(date -> date.isAfter(dateInPast))
                .collect(groupingBy(o -> o, counting()))
                .entrySet()
                .stream()
                .map(entry -> new NumberOfItemByDateWrapper(entry.getKey(), entry.getValue()))
                .collect(toSet());
    }

}
