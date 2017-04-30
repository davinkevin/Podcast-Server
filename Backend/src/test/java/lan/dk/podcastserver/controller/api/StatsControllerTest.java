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

    private @Mock StatsBusiness statsBusiness;
    private @InjectMocks StatsController statsController;

    @Test
    public void should_find_stats_for_all_podcast_by_download_date() {
        /* Given */
        Integer numberOfMonth = 6;
        List<StatsPodcastType> stats = new ArrayList<>();
        when(statsBusiness.allStatsByTypeAndDownloadDate(eq(6))).thenReturn(stats);

        /* When */
        List<StatsPodcastType> statsByDownloadDate = statsController.byDownloadDate(numberOfMonth);

        /* Then */
        assertThat(stats).isSameAs(statsByDownloadDate);
        verify(statsBusiness, only()).allStatsByTypeAndDownloadDate(eq(numberOfMonth));
    }

    @Test
    public void should_find_stats_for_all_podcast_by_creation_date() {
        /* Given */
        Integer numberOfMonth = 6;
        List<StatsPodcastType> stats = new ArrayList<>();
        when(statsBusiness.allStatsByTypeAndCreationDate(6)).thenReturn(stats);

        /* When */
        List<StatsPodcastType> statsByCreationDate = statsController.byCreationDate(numberOfMonth);

        /* Then */
        assertThat(stats).isSameAs(statsByCreationDate);
        verify(statsBusiness, only()).allStatsByTypeAndCreationDate(eq(numberOfMonth));
    }



}