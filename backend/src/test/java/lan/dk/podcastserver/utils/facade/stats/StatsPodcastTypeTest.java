package lan.dk.podcastserver.utils.facade.stats;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper;
import lan.dk.podcastserver.business.stats.StatsPodcastType;
import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 01/07/15 for Podcast Server
 */
public class StatsPodcastTypeTest {


    private static final NumberOfItemByDateWrapper NI_1 = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 100);
    private static final NumberOfItemByDateWrapper NI_2 = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 2), 200);
    private static final NumberOfItemByDateWrapper NI_3 = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 3), 300);
    private static final String FAKE_TYPE = "FakeType";

    @Test
    public void should_has_correct_value() {
        /* Given */
        Set<NumberOfItemByDateWrapper> numberOfItemSets = HashSet.of(NI_1, NI_2, NI_3);

        StatsPodcastType statsPodcastType = new StatsPodcastType(FAKE_TYPE, numberOfItemSets);

        /* When */
        String type = statsPodcastType.getType();
        Set<NumberOfItemByDateWrapper> values = statsPodcastType.getValues();

        /* Then */
        assertThat(type).isEqualTo(FAKE_TYPE);
        assertThat(values).contains(NI_1, NI_2, NI_3);
    }
}
