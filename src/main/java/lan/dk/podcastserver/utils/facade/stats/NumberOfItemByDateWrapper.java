package lan.dk.podcastserver.utils.facade.stats;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDate;

/**
 * Created by kevin on 06/04/15.
 */
public class NumberOfItemByDateWrapper {

    private LocalDate date;
    private Long numberOfItems;

    public NumberOfItemByDateWrapper(LocalDate date, Long numberOfItems) {
        this.date = date;
        this.numberOfItems = numberOfItems;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getNumberOfItems() {
        return numberOfItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberOfItemByDateWrapper)) return false;

        NumberOfItemByDateWrapper that = (NumberOfItemByDateWrapper) o;
        return new EqualsBuilder()
                .append(date, that.date)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(date)
                .toHashCode();
    }
}
