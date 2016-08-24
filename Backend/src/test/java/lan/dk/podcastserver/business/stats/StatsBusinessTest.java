package lan.dk.podcastserver.business.stats;

import com.google.common.collect.Sets;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
    private static final AbstractUpdater.Type YOUTUBE = new AbstractUpdater.Type("Youtube", "Youtube");
    private static final AbstractUpdater.Type CANAL_PLUS = new AbstractUpdater.Type("CanalPlus", "CanalPlus");
    private static final AbstractUpdater.Type BE_IN_SPORT = new AbstractUpdater.Type("BeInSport", "BeInSport");
    private static final AbstractUpdater.Type RSS = new AbstractUpdater.Type("RSS", "RSS");

    @Test
    public void should_stats_all_by_download_date() {
        /* Given */
        when(updaterSelector.types()).thenReturn(Sets.newHashSet(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE));

        when(itemRepository.findByTypeAndExpression(eq(RSS), any(BooleanExpression.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndExpression(eq(BE_IN_SPORT), any(BooleanExpression.class))).thenReturn(Sets.newHashSet());
        when(itemRepository.findByTypeAndExpression(eq(CANAL_PLUS), any(BooleanExpression.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndExpression(eq(YOUTUBE), any(BooleanExpression.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByTypeAndDownloadDate(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).values()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).values()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).values()).hasSize(50);
    }

    @Test
    public void should_stats_all_by_creation_date() {
        /* Given */
        when(updaterSelector.types()).thenReturn(Sets.newHashSet(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE));

        when(itemRepository.findByTypeAndExpression(eq(RSS), any(BooleanExpression.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndExpression(eq(BE_IN_SPORT), any(BooleanExpression.class))).thenReturn(Sets.newHashSet());
        when(itemRepository.findByTypeAndExpression(eq(CANAL_PLUS), any(BooleanExpression.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndExpression(eq(YOUTUBE), any(BooleanExpression.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByTypeAndCreationDate(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).values()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).values()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).values()).hasSize(50);
    }

    @Test
    public void should_stats_all_by_publication_date() {
        /* Given */
        when(updaterSelector.types()).thenReturn(Sets.newHashSet(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE));

        when(itemRepository.findByTypeAndExpression(eq(RSS), any(BooleanExpression.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndExpression(eq(BE_IN_SPORT), any(BooleanExpression.class))).thenReturn(Sets.newHashSet());
        when(itemRepository.findByTypeAndExpression(eq(CANAL_PLUS), any(BooleanExpression.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndExpression(eq(YOUTUBE), any(BooleanExpression.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByTypeAndPubDate(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).values()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).values()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).values()).hasSize(50);
    }

    private Set<Item> generateItems(Integer numberOfItem) {
        return IntStream.rangeClosed(1, numberOfItem)
                // FlatMap To object to avoid repartion 1 per date
                .mapToObj(i -> new Item()
                        .setId(UUID.randomUUID())
                        .setPubDate(ZonedDateTime.now().minusDays(i))
                        .setDownloadDate(ZonedDateTime.now().minusDays(i))
                        .setCreationDate(ZonedDateTime.now().minusDays(i))
                )
                .collect(toSet());
    }

    @Test
    public void should_generate_stats_by_downloadDate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast().setItems(generateItems(356));
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statsByDownloadDate(UUID.randomUUID(), 6L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(6), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers).hasSize(days.intValue());
    }


    @Test
    public void should_generate_stats_by_pubdate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast().setItems(generateItems(356));
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statsByPubDate(UUID.randomUUID(), 2L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(2), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers).hasSize(days.intValue());
    }

    @Test
    public void should_generate_stats_by_creationdate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast().setItems(generateItems(356));
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statsByCreationDate(UUID.randomUUID(), 2L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(2), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers).hasSize(days.intValue());
    }
}