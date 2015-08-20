package lan.dk.podcastserver.business.stats;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.service.WorkerService;
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

    @Mock ItemBusiness itemBusiness;
    @Mock PodcastBusiness podcastBusiness;
    @Mock WorkerService workerService;
    @InjectMocks StatsBusiness statsBusiness;

    @Test
    public void should_stats_all_by_type() {
        /* Given */
        AbstractUpdater.Type rss = new AbstractUpdater.Type("RSS", "RSS");
        AbstractUpdater.Type beInSport = new AbstractUpdater.Type("BeInSport", "BeInSport");
        AbstractUpdater.Type canalPlus = new AbstractUpdater.Type("CanalPlus", "CanalPlus");
        AbstractUpdater.Type youtube = new AbstractUpdater.Type("Youtube", "Youtube");
        when(workerService.types()).thenReturn(new HashSet<>(Arrays.asList(rss, beInSport, canalPlus, youtube)));

        when(itemBusiness.findByTypeAndDownloadDateAfter(eq(rss), any(ZonedDateTime.class))).thenReturn(generateItems(5));
        when(itemBusiness.findByTypeAndDownloadDateAfter(eq(beInSport), any(ZonedDateTime.class))).thenReturn(new ArrayList<>());
        when(itemBusiness.findByTypeAndDownloadDateAfter(eq(canalPlus), any(ZonedDateTime.class))).thenReturn(generateItems(10));
        when(itemBusiness.findByTypeAndDownloadDateAfter(eq(youtube), any(ZonedDateTime.class))).thenReturn(generateItems(50));

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
                        .setId(i)
                        .setPubdate(ZonedDateTime.now().minusDays(i))
                        .setDownloadDate(ZonedDateTime.now().minusDays(i)))
                .collect(toList());
    }

    @Test
    public void should_generate_stats_by_downloadDate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>(generateItems(356)));
        when(podcastBusiness.findOne(anyInt())).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statsByDownloadDate(123, 6L);

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
        when(podcastBusiness.findOne(anyInt())).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statByPubDate(6, 2L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(2), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers)
                .hasSize(days.intValue());
    }
}