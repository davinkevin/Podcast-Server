package lan.dk.podcastserver.utils.facade.stats;

import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 01/07/15 for Podcast Server
 */
public class StatsPodcastTypeTest {


    public static final NumberOfItemByDateWrapper NI_1 = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 100L);
    public static final NumberOfItemByDateWrapper NI_2 = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 2), 200L);
    public static final NumberOfItemByDateWrapper NI_3 = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 3), 300L);
    public static final String FAKE_TYPE = "FakeType";

    @Test
    public void should_has_correct_value() {
        /* Given */
        Set<NumberOfItemByDateWrapper> numberOfItemSets = new HashSet<>();
        numberOfItemSets.add(NI_1);
        numberOfItemSets.add(NI_2);
        numberOfItemSets.add(NI_3);

        StatsPodcastType statsPodcastType = new StatsPodcastType(FAKE_TYPE, numberOfItemSets);

        /* When */
        String type = statsPodcastType.type();
        Set<NumberOfItemByDateWrapper> values = statsPodcastType.values();

        /* Then */
        assertThat(type).isEqualTo(FAKE_TYPE);
        assertThat(values).contains(NI_1, NI_2, NI_3);
    }
}