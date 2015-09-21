package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.stats.StatsBusiness;
import lan.dk.podcastserver.utils.facade.stats.StatsPodcastType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 21/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class StatsControllerTest {

    @Mock StatsBusiness itemStatsBusiness;
    @InjectMocks StatsController statsController;

    @Test
    public void should_find_stats_for_all_podcast() {
        /* Given */
        Integer numberOfMonth = 6;
        List<StatsPodcastType> statsPodcastTypes = new ArrayList<>();
        when(itemStatsBusiness.allStatsByType(eq(6))).thenReturn(statsPodcastTypes);

        /* When */
        List<StatsPodcastType> statsByType = statsController.statsByType(numberOfMonth);

        /* Then */
        assertThat(statsPodcastTypes).isSameAs(statsByType);
        verify(itemStatsBusiness, only()).allStatsByType(eq(numberOfMonth));
    }

}