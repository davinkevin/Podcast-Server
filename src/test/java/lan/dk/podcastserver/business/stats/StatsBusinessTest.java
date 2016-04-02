package lan.dk.podcastserver.business.stats;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.utils.facade.stats.NumberOfItemByDateWrapper;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
/**
 * Created by kevin on 05/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class StatsBusinessTest {

    @Mock ItemRepository itemRepository;
    @Mock PodcastBusiness podcastBusiness;
    @Mock UpdaterSelector updaterSelector;
    @InjectMocks StatsBusiness statsBusiness;

    @Test
    public void should_stats_all_by_type() {
        /* Given */
        AbstractUpdater.Type rss = new AbstractUpdater.Type("RSS", "RSS");
        AbstractUpdater.Type beInSport = new AbstractUpdater.Type("BeInSport", "BeInSport");
        AbstractUpdater.Type canalPlus = new AbstractUpdater.Type("CanalPlus", "CanalPlus");
        AbstractUpdater.Type youtube = new AbstractUpdater.Type("Youtube", "Youtube");
        when(updaterSelector.types()).thenReturn(Sets.newHashSet(rss, beInSport, canalPlus, youtube));

        when(itemRepository.findByTypeAndDownloadDateAfter(eq(rss), any(ZonedDateTime.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndDownloadDateAfter(eq(beInSport), any(ZonedDateTime.class))).thenReturn(new ArrayList<>());
        when(itemRepository.findByTypeAndDownloadDateAfter(eq(canalPlus), any(ZonedDateTime.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndDownloadDateAfter(eq(youtube), any(ZonedDateTime.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByType(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).values()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).values()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).values()).hasSize(50);
    }

    private List<Item> generateItems(Integer numberOfItem) {
        return IntStream.rangeClosed(1, numberOfItem)
                // FlatMap To object to avoid repartion 1 per date
                .mapToObj(i -> new Item()
                        .setId(UUID.randomUUID())
                        .setPubdate(ZonedDateTime.now().minusDays(i))
                        .setDownloadDate(ZonedDateTime.now().minusDays(i)))
                .collect(toList());
    }

    @Test
    public void should_generate_stats_by_downloadDate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>(generateItems(356)));
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statsByDownloadDate(UUID.randomUUID(), 6L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(6), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers)
                .hasSize(days.intValue());
    }


    @Test
    public void should_generate_stats_by_pubdate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>(generateItems(356)));
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statByPubDate(UUID.randomUUID(), 2L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(2), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers)
                .hasSize(days.intValue());
    }
}