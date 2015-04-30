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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.*;
import static lan.dk.podcastserver.repository.specification.ItemSpecifications.hasBeendDownloadedAfter;
import static lan.dk.podcastserver.repository.specification.ItemSpecifications.isOfType;

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Component
@Transactional(readOnly = true)
public class ItemStatsBusiness {

    ItemBusiness itemBusiness;
    PodcastBusiness podcastBusiness;

    @Autowired
    public ItemStatsBusiness(ItemBusiness itemBusiness, PodcastBusiness podcastBusiness) {
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


}
