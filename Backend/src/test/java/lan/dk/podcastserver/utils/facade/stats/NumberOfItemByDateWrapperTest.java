package lan.dk.podcastserver.utils.facade.stats;

import lan.dk.podcastserver.business.stats.NumberOfItemByDateWrapper;
import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 01/07/15 for Podcast Server
 */
public class NumberOfItemByDateWrapperTest {

    private NumberOfItemByDateWrapper numberOfItemByDateWrapper = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 100);
    
    @Test
    public void should_have_the_correct_value() {
        assertThat(numberOfItemByDateWrapper.getDate()).isEqualTo(LocalDate.of(2015, Month.JULY, 1));
        assertThat(numberOfItemByDateWrapper.getNumberOfItems()).isEqualTo(100);
    }

    @Test
    public void should_be_equals_and_have_the_same_hashcode() {
        /* Given */ NumberOfItemByDateWrapper copynoibdw = new NumberOfItemByDateWrapper(LocalDate.of(2015, Month.JULY, 1), 200);
        /* Then */
        assertThat(numberOfItemByDateWrapper).isEqualTo(copynoibdw);
        assertThat(numberOfItemByDateWrapper.hashCode()).isEqualTo(copynoibdw.hashCode());
    }
}
