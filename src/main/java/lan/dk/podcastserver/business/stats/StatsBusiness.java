package lan.dk.podcastserver.business.stats;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static lan.dk.podcastserver.repository.specification.ItemSpecifications.hasBeendDownloadedAfter;
import static lan.dk.podcastserver.repository.specification.ItemSpecifications.isOfType;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Component
@Transactional(readOnly = true)
public class StatsBusiness {

    ItemBusiness itemBusiness;
    PodcastBusiness podcastBusiness;

    @Autowired
    public StatsBusiness(ItemBusiness itemBusiness, PodcastBusiness podcastBusiness) {
        this.itemBusiness = itemBusiness;
        this.podcastBusiness = podcastBusiness;
    }

    @SuppressWarnings("unchecked")
    public StatsPodcastType generateForType(String type, Integer numberOfMonth) {

        ZonedDateTime dateInPast = ZonedDateTime.now().minusMonths(numberOfMonth);

        Set<NumberOfItemByDateWrapper> values =
                ((List<Item>)itemBusiness.findAll(isOfType(type).and(hasBeendDownloadedAfter(dateInPast))))
                .stream()
                .map(Item::getDownloadDate)
                .filter(Objects::nonNull)
                .map(ZonedDateTime::toLocalDate)
                .collect(groupingBy(o -> o, counting()))
                .entrySet()
                .stream()
                .map(entry -> new NumberOfItemByDateWrapper(entry.getKey(), entry.getValue()))
                .collect(toSet());

        return new StatsPodcastType(type, values);
    }

    public List<StatsPodcastType> allStatsByType(Integer numberOfMonth) {
        return podcastBusiness
                .findAll()
                .stream()
                .map(Podcast::getType)
                .distinct()
                .map(type -> generateForType(type, numberOfMonth))
                .collect(toList());
    }

    public Set<NumberOfItemByDateWrapper> statByPubDate(Integer podcastId, Long numberOfMonth) {
        return statOf(podcastId, Item::getPubdate, numberOfMonth);
    }

    public Set<NumberOfItemByDateWrapper> statsByDownloadDate(Integer id, Long numberOfMonth) {
        return statOf(id, Item::getDownloadDate, numberOfMonth);
    }

    @SuppressWarnings("unchecked")
    private Set<NumberOfItemByDateWrapper> statOf(Integer podcastId, Function<? extends Item, ? extends ZonedDateTime> mapper, long numberOfMonth) {
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
