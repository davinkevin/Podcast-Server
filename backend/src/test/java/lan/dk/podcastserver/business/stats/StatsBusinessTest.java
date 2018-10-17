package lan.dk.podcastserver.business.stats;

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper;
import com.github.davinkevin.podcastserver.business.stats.StatsPodcastType;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.repository.ItemRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
/**
 * Created by kevin on 05/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class StatsBusinessTest {

    private @Mock ItemRepository itemRepository;
    private @Mock PodcastBusiness podcastBusiness;
    private @Mock UpdaterSelector updaterSelector;
    private @InjectMocks StatsBusiness statsBusiness;

    private static final Type YOUTUBE = new Type("Youtube", "Youtube");
    private static final Type CANAL_PLUS = new Type("CanalPlus", "CanalPlus");
    private static final Type BE_IN_SPORT = new Type("BeInSport", "BeInSport");
    private static final Type RSS = new Type("RSS", "RSS");

    @Test
    public void should_stats_all_by_download_date() {
        /* Given */
        when(updaterSelector.types()).thenReturn(HashSet.of(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE));

        when(itemRepository.findByTypeAndExpression(eq(RSS), any(BooleanExpression.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndExpression(eq(BE_IN_SPORT), any(BooleanExpression.class))).thenReturn(HashSet.empty());
        when(itemRepository.findByTypeAndExpression(eq(CANAL_PLUS), any(BooleanExpression.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndExpression(eq(YOUTUBE), any(BooleanExpression.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByTypeAndDownloadDate(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).getValues()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).getValues()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).getValues()).hasSize(50);
    }

    @Test
    public void should_stats_all_by_creation_date() {
        /* Given */
        when(updaterSelector.types()).thenReturn(HashSet.of(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE));

        when(itemRepository.findByTypeAndExpression(eq(RSS), any(BooleanExpression.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndExpression(eq(BE_IN_SPORT), any(BooleanExpression.class))).thenReturn(HashSet.empty());
        when(itemRepository.findByTypeAndExpression(eq(CANAL_PLUS), any(BooleanExpression.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndExpression(eq(YOUTUBE), any(BooleanExpression.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByTypeAndCreationDate(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).getValues()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).getValues()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).getValues()).hasSize(50);
    }

    @Test
    public void should_stats_all_by_publication_date() {
        /* Given */
        when(updaterSelector.types()).thenReturn(HashSet.of(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE));

        when(itemRepository.findByTypeAndExpression(eq(RSS), any(BooleanExpression.class))).thenReturn(generateItems(5));
        when(itemRepository.findByTypeAndExpression(eq(BE_IN_SPORT), any(BooleanExpression.class))).thenReturn(HashSet.empty());
        when(itemRepository.findByTypeAndExpression(eq(CANAL_PLUS), any(BooleanExpression.class))).thenReturn(generateItems(10));
        when(itemRepository.findByTypeAndExpression(eq(YOUTUBE), any(BooleanExpression.class))).thenReturn(generateItems(50));

        /* When */
        List<StatsPodcastType> statsPodcastTypes = statsBusiness.allStatsByTypeAndPubDate(1);

        /* Then */
        assertThat(statsPodcastTypes).hasSize(3);
        assertThat(statsPodcastTypes.get(0).getValues()).hasSize(10);
        assertThat(statsPodcastTypes.get(1).getValues()).hasSize(5);
        assertThat(statsPodcastTypes.get(2).getValues()).hasSize(50);
    }

    private Set<Item> generateItems(Integer numberOfItem) {
        return HashSet.rangeClosed(1, numberOfItem)
                // FlatMap To object to avoid repartion 1 per date
                .map(i -> new Item()
                        .setId(UUID.randomUUID())
                        .setPubDate(ZonedDateTime.now().minusDays(i))
                        .setDownloadDate(ZonedDateTime.now().minusDays(i))
                        .setCreationDate(ZonedDateTime.now().minusDays(i))
                );
    }

    @Test
    public void should_generate_stats_by_downloadDate_for_podcast() {
        /* Given */
        Podcast podcast = new Podcast().setItems(generateItems(356).toJavaSet());
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
        Podcast podcast = new Podcast().setItems(generateItems(356).toJavaSet());
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
        Podcast podcast = new Podcast().setItems(generateItems(356).toJavaSet());
        when(podcastBusiness.findOne(any(UUID.class))).thenReturn(podcast);

        /* When */
        Set<NumberOfItemByDateWrapper> numberOfItemByDateWrappers = statsBusiness.statsByCreationDate(UUID.randomUUID(), 2L);

        /* Then */
        Long days = DAYS.between(LocalDate.now().minusMonths(2), LocalDate.now())-1;
        assertThat(numberOfItemByDateWrappers).hasSize(days.intValue());
    }
}
